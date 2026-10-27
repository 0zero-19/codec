package io.codec.media

import io.codec.model.*
import java.io.File

class FFmpegCommandBuilder(
    private val ffmpegExecutable: String = FFmpegCapabilities.findFFmpegExecutable()
) {
    fun buildCommand(
        inputPath: String,
        outputPath: String,
        options: ConversionOptions,
        mediaInfo: MediaInfo?,
        pass: Int = 1,
        totalPasses: Int = 1,
        passLogFilePrefix: String? = null
    ): List<String> {
        val args = mutableListOf<String>()
        args.add(ffmpegExecutable)
        args.add("-hide_banner")
        args.add("-y") // Overwrite output safely (queue manages output file collisions)

        // Trimming - fast seek before input if possible
        options.trimStartTimeSeconds?.let { start ->
            if (start > 0) {
                args.add("-ss")
                args.add(String.format(java.util.Locale.US, "%.3f", start))
            }
        }

        // Input
        args.add("-i")
        args.add(inputPath)

        // Trim end / duration
        options.trimEndTimeSeconds?.let { end ->
            val start = options.trimStartTimeSeconds ?: 0.0
            val duration = end - start
            if (duration > 0) {
                args.add("-t")
                args.add(String.format(java.util.Locale.US, "%.3f", duration))
            }
        }

        val isGif = options.video.container == ContainerFormat.GIF

        // Stream mapping: explicitly map selected streams to prevent unintended stream loss/selection
        if (options.video.selectedVideoStreamIndex != null) {
            args.add("-map")
            args.add("0:v:${options.video.selectedVideoStreamIndex}")
        } else if (!options.video.container.isAudioOnly && !isGif && options.video.codec != VideoCodec.NONE) {
            args.add("-map")
            args.add("0:v:0?")
        }

        if (options.audio.selectedAudioStreamIndex != null) {
            args.add("-map")
            args.add("0:a:${options.audio.selectedAudioStreamIndex}")
        } else if (!isGif && !options.audio.stripAudio && options.audio.codec != AudioCodec.NONE && !(totalPasses == 2 && pass == 1)) {
            args.add("-map")
            args.add("0:a:0?")
        }

        // Video settings
        if (isGif) {
            buildGifEncoding(args, options)
        } else if (options.video.container.isAudioOnly) {
            args.add("-vn")
        } else {
            buildVideoEncoding(args, options, mediaInfo, pass, totalPasses, passLogFilePrefix)
        }

        // Audio settings
        if (isGif || options.audio.stripAudio || options.audio.codec == AudioCodec.NONE || (totalPasses == 2 && pass == 1)) {
            args.add("-an")
        } else {
            buildAudioEncoding(args, options)
        }

        // Subtitle settings
        if (!isGif && !options.video.container.isAudioOnly) {
            buildSubtitleEncoding(args, options)
        }

        // MP4 / MOV Web Optimization (Faststart)
        if (options.video.container == ContainerFormat.MP4 || options.video.container == ContainerFormat.MOV || options.video.container == ContainerFormat.M4V) {
            args.add("-movflags")
            args.add("+faststart")
        }

        // Metadata
        if (options.stripMetadata) {
            args.add("-map_metadata")
            args.add("-1")
        }

        // Custom extra FFmpeg arguments (safely parsed tokens)
        options.customFfmpegArgs?.let { customArgs ->
            val tokens = parseCustomArgs(customArgs)
            args.addAll(tokens)
        }

        // Real-time progress output to stdout
        args.add("-progress")
        args.add("pipe:1")
        args.add("-nostats")

        // Output file or null for pass 1 of 2-pass
        if (totalPasses == 2 && pass == 1) {
            args.add("-f")
            args.add("null")
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            args.add(if (isWindows) "NUL" else "/dev/null")
        } else {
            args.add(outputPath)
        }

        return args
    }

    private fun buildGifEncoding(args: MutableList<String>, options: ConversionOptions) {
        val targetFps = options.video.targetFps?.takeIf { it > 0 } ?: 15.0
        val (targetWidth, targetHeight) = resolveTargetDimensions(options.video, null)
        val scaleStr = if (targetWidth > 0 && targetHeight > 0) {
            "scale=$targetWidth:$targetHeight:flags=lanczos"
        } else {
            "scale=iw:ih:flags=lanczos"
        }

        // High quality GIF with palettegen and paletteuse
        val filterGraph = "fps=$targetFps,$scaleStr,split[s0][s1];[s0]palettegen=max_colors=256:stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3"
        args.add("-filter_complex")
        args.add(filterGraph)
    }

    private fun buildVideoEncoding(
        args: MutableList<String>,
        options: ConversionOptions,
        mediaInfo: MediaInfo?,
        pass: Int,
        totalPasses: Int,
        passLogFilePrefix: String?
    ) {
        val video = options.video

        if (video.codec == VideoCodec.COPY) {
            args.add("-c:v")
            args.add("copy")
            return
        }

        if (video.codec == VideoCodec.NONE) {
            args.add("-vn")
            return
        }

        // Resolve encoder name based on hardware preference
        val encoder = resolveEncoderName(video)
        args.add("-c:v")
        args.add(encoder)

        // Rate control and Quality
        when (video.qualityMode) {
            QualityMode.CRF -> {
                when {
                    encoder.contains("nvenc") -> {
                        args.add("-rc")
                        args.add("vbr")
                        args.add("-cq")
                        args.add(video.crf.toString())
                    }
                    encoder.contains("qsv") -> {
                        args.add("-global_quality")
                        args.add(video.crf.toString())
                    }
                    encoder.contains("amf") -> {
                        args.add("-rc")
                        args.add("cqp")
                        args.add("-qp_i")
                        args.add(video.crf.toString())
                        args.add("-qp_p")
                        args.add(video.crf.toString())
                    }
                    else -> {
                        args.add("-crf")
                        args.add(video.crf.toString())
                    }
                }
            }

            QualityMode.BITRATE -> {
                args.add("-b:v")
                args.add("${video.videoBitrateKbps}k")
                args.add("-maxrate")
                args.add("${(video.videoBitrateKbps * 1.5).toInt()}k")
                args.add("-bufsize")
                args.add("${video.videoBitrateKbps * 2}k")
            }

            QualityMode.TARGET_SIZE -> {
                val targetBitrate = calculateTargetBitrateKbps(options, mediaInfo)
                args.add("-b:v")
                args.add("${targetBitrate}k")
                args.add("-maxrate")
                args.add("${(targetBitrate * 1.3).toInt()}k")
                args.add("-bufsize")
                args.add("${targetBitrate * 2}k")
            }

            QualityMode.TWO_PASS -> {
                val targetMb = video.targetSizeMb
                val bitrate = if (targetMb != null && targetMb > 0) {
                    calculateTargetBitrateKbps(options, mediaInfo)
                } else {
                    video.videoBitrateKbps
                }

                args.add("-b:v")
                args.add("${bitrate}k")

                if (totalPasses == 2) {
                    args.add("-pass")
                    args.add(pass.toString())
                    passLogFilePrefix?.let {
                        args.add("-passlogfile")
                        args.add(it)
                    }
                }
            }
        }

        // Preset speed
        if (!encoder.contains("copy")) {
            args.add("-preset")
            args.add(video.preset.value)
        }

        // Tune
        if (video.tune != Tune.NONE && (encoder == "libx264" || encoder == "libx265")) {
            args.add("-tune")
            args.add(video.tune.value)
        }

        // Profile & Level
        video.profile?.let {
            if (it.isNotBlank()) {
                args.add("-profile:v")
                args.add(it)
            }
        }
        video.level?.let {
            if (it.isNotBlank()) {
                args.add("-level:v")
                args.add(it)
            }
        }

        // GOP & B-frames
        video.gopSize?.let {
            if (it > 0) {
                args.add("-g")
                args.add(it.toString())
            }
        }
        video.bFrames?.let {
            if (it >= 0) {
                args.add("-bf")
                args.add(it.toString())
            }
        }

        // Pixel format (yuv420p default for compatibility)
        video.pixelFormat?.let {
            if (it.isNotBlank()) {
                args.add("-pix_fmt")
                args.add(it)
            }
        }

        // Tag for Apple/Windows HEVC compatibility in MP4 containers
        if ((video.codec == VideoCodec.H265 || encoder.contains("hevc") || encoder.contains("265")) &&
            (options.video.container == ContainerFormat.MP4 || options.video.container == ContainerFormat.M4V || options.video.container == ContainerFormat.MOV)) {
            args.add("-tag:v")
            args.add("hvc1")
        }

        // Video Filter chain
        val filters = buildVideoFilterChain(options, mediaInfo)
        if (filters.isNotEmpty()) {
            args.add("-vf")
            args.add(filters.joinToString(","))
        }
    }

    private fun buildVideoFilterChain(options: ConversionOptions, mediaInfo: MediaInfo?): List<String> {
        val filters = mutableListOf<String>()
        val video = options.video
        val filter = options.filter

        // 1. Deinterlace
        if (filter.deinterlace) {
            filters.add("yadif=mode=send_frame:parity=auto:deint=all")
        }

        // 2. Crop
        if (filter.cropEnabled && filter.cropWidth > 0 && filter.cropHeight > 0) {
            filters.add("crop=${filter.cropWidth}:${filter.cropHeight}:${filter.cropX}:${filter.cropY}")
        }

        // 3. Rotation & Flip
        when (filter.rotate) {
            90 -> filters.add("transpose=1")
            180 -> filters.add("transpose=2,transpose=2")
            270 -> filters.add("transpose=2")
        }
        if (filter.hFlip) filters.add("hflip")
        if (filter.vFlip) filters.add("vflip")

        // 4. HDR to SDR Tonemapping
        if (video.hdrToSdrTonemap) {
            filters.add("zscale=t=linear:npl=100,format=gbrpf32le,zscale=p=bt709,tonemap=tonemap=mobius:desat=0,zscale=t=bt709:m=bt709:r=tv,format=yuv420p")
        }

        // 5. Scaling
        val (targetW, targetH) = resolveTargetDimensions(video, mediaInfo)
        if (targetW > 0 && targetH > 0) {
            if (video.keepAspectRatio) {
                filters.add("scale=$targetW:$targetH:force_original_aspect_ratio=decrease,pad=$targetW:$targetH:(ow-iw)/2:(oh-ih)/2")
            } else {
                filters.add("scale=$targetW:$targetH")
            }
        }

        // 6. Framerate
        val targetFps = video.targetFps
        if (targetFps != null && targetFps > 0) {
            filters.add("fps=fps=$targetFps")
        }

        // 7. Denoise
        if (filter.denoise) {
            filters.add("hqdn3d=4:3:6:4.5")
        }

        // 8. Sharpen
        if (filter.sharpen) {
            filters.add("unsharp=5:5:1.0:5:5:0.0")
        }

        // 9. Equalizer / Color correction
        val hasEq = filter.brightness != 0.0f || filter.contrast != 1.0f || filter.saturation != 1.0f || filter.gamma != 1.0f
        if (hasEq) {
            filters.add(
                String.format(
                    java.util.Locale.US,
                    "eq=brightness=%.2f:contrast=%.2f:saturation=%.2f:gamma=%.2f",
                    filter.brightness,
                    filter.contrast,
                    filter.saturation,
                    filter.gamma
                )
            )
        }

        // 10. Subtitle Burn-In
        if (options.subtitle.mode == SubtitleMode.BURN_IN) {
            val subPath = options.subtitle.externalSubtitlePath
            if (!subPath.isNullOrBlank()) {
                val escapedPath = subPath.replace("\\", "/").replace(":", "\\:")
                filters.add("subtitles='$escapedPath'")
            }
        }

        return filters
    }

    private fun buildAudioEncoding(args: MutableList<String>, options: ConversionOptions) {
        val audio = options.audio

        if (audio.codec == AudioCodec.COPY) {
            args.add("-c:a")
            args.add("copy")
            return
        }

        args.add("-c:a")
        args.add(audio.codec.encoderName)

        if (audio.codec != AudioCodec.FLAC && audio.codec != AudioCodec.WAV_PCM) {
            args.add("-b:a")
            args.add("${audio.bitrateKbps}k")
        }

        if (audio.sampleRateHz > 0) {
            args.add("-ar")
            args.add(audio.sampleRateHz.toString())
        }

        if (audio.channels != ChannelMode.KEEP_SOURCE) {
            args.add("-ac")
            args.add(audio.channels.channels.toString())
        }

        // Audio Filter Chain
        val aFilters = mutableListOf<String>()

        if (audio.volumeMultiplier != 1.0f && audio.volumeMultiplier > 0.0f) {
            aFilters.add(String.format(java.util.Locale.US, "volume=%.2f", audio.volumeMultiplier))
        }

        if (audio.normalizeLoudness) {
            aFilters.add("loudnorm=I=-16:TP=-1.5:LRA=11")
        } else if (audio.dynamicAudioNormalizer) {
            aFilters.add("dynaudnorm=f=150:g=15")
        }

        if (aFilters.isNotEmpty()) {
            args.add("-af")
            args.add(aFilters.joinToString(","))
        }
    }

    private fun buildSubtitleEncoding(args: MutableList<String>, options: ConversionOptions) {
        when (options.subtitle.mode) {
            SubtitleMode.NONE -> {
                // Keep default or strip
            }
            SubtitleMode.EMBED_TRACK -> {
                options.subtitle.externalSubtitlePath?.let { extSub ->
                    args.add("-i")
                    args.add(extSub)
                    args.add("-map")
                    args.add("0:v")
                    args.add("-map")
                    args.add("0:a?")
                    args.add("-map")
                    args.add("1:s?")
                    args.add("-c:s")
                    args.add(if (options.video.container == ContainerFormat.MP4) "mov_text" else "srt")
                }
            }
            SubtitleMode.BURN_IN -> {
                // Handled in video filter chain
            }
        }
    }

    private fun resolveEncoderName(video: VideoSettings): String {
        return when (video.hardwareEncoder) {
            HardwareEncoder.NVENC -> when (video.codec) {
                VideoCodec.H264 -> "h264_nvenc"
                VideoCodec.H265 -> "hevc_nvenc"
                VideoCodec.AV1 -> "av1_nvenc"
                else -> video.codec.encoderName
            }
            HardwareEncoder.QSV -> when (video.codec) {
                VideoCodec.H264 -> "h264_qsv"
                VideoCodec.H265 -> "hevc_qsv"
                VideoCodec.AV1 -> "av1_qsv"
                else -> video.codec.encoderName
            }
            HardwareEncoder.AMF -> when (video.codec) {
                VideoCodec.H264 -> "h264_amf"
                VideoCodec.H265 -> "hevc_amf"
                VideoCodec.AV1 -> "av1_amf"
                else -> video.codec.encoderName
            }
            HardwareEncoder.VAAPI -> when (video.codec) {
                VideoCodec.H264 -> "h264_vaapi"
                VideoCodec.H265 -> "hevc_vaapi"
                else -> video.codec.encoderName
            }
            HardwareEncoder.MEDIACODEC -> when (video.codec) {
                VideoCodec.H264 -> "h264_mediacodec"
                VideoCodec.H265 -> "hevc_mediacodec"
                else -> video.codec.encoderName
            }
            HardwareEncoder.SOFTWARE -> video.codec.encoderName
            HardwareEncoder.AUTO -> video.codec.encoderName // default to safe software unless user explicitly configures hw
        }
    }

    private fun resolveTargetDimensions(video: VideoSettings, mediaInfo: MediaInfo?): Pair<Int, Int> {
        val (rawW, rawH) = when (video.resolutionPreset) {
            ResolutionPreset.ORIGINAL -> {
                val orig = mediaInfo?.primaryVideoStream
                if (orig != null && orig.width > 0 && orig.height > 0) {
                    orig.width to orig.height
                } else {
                    0 to 0
                }
            }
            ResolutionPreset.CUSTOM -> {
                val w = video.customWidth ?: 0
                val h = video.customHeight ?: 0
                w to h
            }
            else -> {
                video.resolutionPreset.width to video.resolutionPreset.height
            }
        }
        val evenW = if (rawW > 0) (rawW / 2) * 2 else 0
        val evenH = if (rawH > 0) (rawH / 2) * 2 else 0
        return evenW to evenH
    }

    fun calculateTargetBitrateKbps(options: ConversionOptions, mediaInfo: MediaInfo?): Int {
        val targetMb = options.video.targetSizeMb ?: return options.video.videoBitrateKbps
        val duration = mediaInfo?.durationSeconds?.takeIf { it > 0 } ?: 60.0
        val audioKbps = if (options.audio.stripAudio || options.audio.codec == AudioCodec.NONE) 0 else options.audio.bitrateKbps
        val totalBits = targetMb * 1024.0 * 1024.0 * 8.0
        val totalKbps = (totalBits / duration) / 1000.0
        val videoKbps = (totalKbps - audioKbps).toInt()
        return maxOf(64, videoKbps)
    }

    private fun parseCustomArgs(custom: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (ch in custom.trim()) {
            when {
                (ch == '"' || ch == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = ch
                }
                ch == quoteChar && inQuotes -> {
                    inQuotes = false
                }
                ch.isWhitespace() && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> {
                    current.append(ch)
                }
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }
}
