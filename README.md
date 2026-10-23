# Codec — Universal Cross-Platform Media Converter & Compressor

**Codec** — это профессиональный, универсальный и полностью автономный медиаконвертер нового поколения для **Windows, Linux и Android**. Построен на базе мощного мультимедийного движка FFmpeg и современного декларативного интерфейса **Compose Multiplatform** (Kotlin Multiplatform).

Приложение разработано как полнофункциональная альтернатива HandBrake с акцентом на скорость работы, эстетику, простоту повседневного использования и широкие возможности точной настройки.

---

## Ключевые возможности

### 1. Архитектура и движок
- **FFmpeg & MediaCodec:** Прямая интеграция с нативным медиадвижком FFmpeg и аппаратными кодеками Android MediaCodec.
- **Аппаратное ускорение:** Поддержка NVIDIA NVENC, Intel Quick Sync Video (QSV), AMD AMF, Apple VideoToolbox и Android Hardware Codecs.
- **Модульная структура:**
  - `:core-model` — предметная модель, кодеки, контейнеры, профили и конфигурации.
  - `:core-media` — построитель команд FFmpeg, парсер прогресса в реальном времени, инспектор ffprobe и утилиты `MediaTools`.
  - `:core-queue` — асинхронная очередь задач с многопоточностью, контролем параллелизма, паузой/отменой/повтором и сохранением на диск.
  - `:core-profiles` — встроенные и пользовательские пресеты (HandBrake, YouTube, Telegram, Discord, Apple, Web).
  - `:core-settings` — персистентное хранилище настроек, путей, тем и параметров производительности.
  - `:desktop` — десктопное приложение для Windows и Linux на Compose Multiplatform.
  - `:android` — мобильное приложение на Compose Material 3 с интеграцией Storage Access Framework (SAF) и Foreground Service.
- **100% Приватность:** Приложение работает локально без интернета, облака, телеметрии и скрытых сборов данных.

### 2. Поддержка форматов
- **Видеоконтейнеры:** MP4, MKV, WebM, MOV, AVI, WMV, FLV, MPEG, TS, M2TS, VOB, OGV, 3GP.
- **Видеокодеки:** H.264/AVC (`libx264`), H.265/HEVC (`libx265`), AV1 (`libsvtav1`), VP8 (`libvpx`), VP9 (`libvpx-vp9`), MPEG-4 (`mpeg4`), Apple ProRes, Lossless Copy (`copy`).
- **Аудиоконтейнеры и кодеки:** MP3 (`libmp3lame`), AAC (`aac`), Opus (`libopus`), FLAC (`flac`), WAV (`pcm_s16le`), OGG Vorbis (`libvorbis`), ALAC, AC3, DTS, AMR, WMA.
- **Инструменты и изображения:** GIF (высококачественный 2-проходный рендеринг через палитру), извлечение кадров в PNG/JPEG, встраивание и прожиг субтитров (SRT, ASS, SSA, mov_text), конкатенация клипов, удаление метаданных.

### 3. Настройки кодирования и сжатия
- **Режимы качества:**
  - Постоянное качество (CRF / Constant Rate Factor) от 0 до 51.
  - Целевой битрейт (Target Bitrate) с возможностью двухпроходного кодирования (2-pass).
  - Режим сжатия до заданного целевого размера (МБ / ГБ) с автоматическим расчётом оптимального битрейта видео и аудио.
- **Параметры энкодера:** Пресеты скорости кодирования (`ultrafast` .. `veryslow`), профили (Baseline, Main, High, Main10), Tune (film, animation, grain, fastdecode, zerolatency), GOP/Keyframes.
- **Фильтры обработки:** Масштабирование (от оригинального до 8K/4K/1080p/720p/480p или произвольного разрешения), изменение частоты кадров (FPS), поворот (90°, 180°, 270°), горизонтальное/вертикальное отражение, деинтерлейсинг (`yadif`), шумоподавление (`hqdn3d`), коррекция резкости (`unsharp`), цветокоррекция (яркость, контраст, насыщенность, гамма через `eq`).

### 4. Очередь и пакетная обработка
- Drag-and-drop импорт отдельных файлов и целых каталогов.
- Пакетное применение готовых пресетов ко всей очереди.
- Детальный мониторинг каждой задачи: точный прогресс (%), скорость относительно реального времени (например, 2.5x), расчётное оставшееся время (ETA), размер исходного и целевого файла.
- Управление: запуск, приостановка, продолжение, отмена, повтор сбойных задач.
- Журнал обработки: просмотр полных логов FFmpeg для каждой задачи прямо в интерфейсе.
- Сохранение состояния очереди в формате JSON при перезапуске приложения.

---

## Интерфейс

### Desktop (Windows & Linux)
- Боковая навигационная панель: **Queue (Очередь)**, **Presets (Пресеты)**, **Media Inspector (Анализатор потоков)**, **Tools (Инструменты)**, **Settings (Настройки)**.
- Центральная рабочая область со списком заданий и инспектором выбранной задачи.
- Выдвижная панель детальных настроек кодирования с вкладками: *Video, Audio, Filters, Subtitles, Advanced*.
- Тёмная и светлая темы оформления с современной палитрой акцентных цветов.
- Локализация: русский и английский языки.

### Android
- Адаптивный мобильный интерфейс в стиле Material Design 3.
- Нижняя панель навигации (Queue, Presets, Tools, Settings).
- Интеграция с Android Storage Access Framework (SAF) для безопасного выбора и экспорта медиа.
- Поддержка системного меню «Поделиться» (Intent `ACTION_SEND`) для быстрой отправки медиафайлов в приложение.
- Foreground Service с постоянным уведомлением в шторке Android для непрерывного кодирования в фоне без убийства системы ОС.

---

## Сборка и запуск

### Требования
- JDK 17 или выше (рекомендуется JDK 21).
- Android SDK (платформа API 34) — для сборки Android APK.
- Наличие `ffmpeg` и `ffprobe` в системном `PATH` (для запуска Desktop версий).

### Сборка для Windows
```bash
# 1. Запуск десктопного приложения в режиме разработки
./gradlew :desktop:run

# 2. Создание переносимой (portable) сборки с встроенным JRE
./gradlew :desktop:createDistributable
# Результат: desktop/build/compose/binaries/main/app/Codec/Codec.exe

# 3. Создание официального инсталлятора MSI
./gradlew :desktop:packageMsi
# Результат: desktop/build/compose/binaries/main/msi/Codec-1.0.0.msi

# 4. Создание EXE инсталлятора
./gradlew :desktop:packageExe
# Результат: desktop/build/compose/binaries/main/exe/Codec-1.0.0.exe

# 5. Сборка универсального UberJar
./gradlew :desktop:packageUberJarForCurrentOS
# Результат: desktop/build/compose/jars/Codec-windows-x64-1.0.0.jar
```

### Сборка для Linux
```bash
# Сборка пакета AppImage (автономный исполняемый файл для любого Linux)
./gradlew :desktop:packageAppImage

# Сборка deb-пакета (Ubuntu, Debian, Linux Mint)
./gradlew :desktop:packageDeb
```

### Сборка для Android
```bash
# Сборка Debug APK
./gradlew :android:assembleDebug
# Результат: android/build/outputs/apk/debug/android-debug.apk

# Сборка Release APK
./gradlew :android:assembleRelease
# Результат: android/build/outputs/apk/release/android-release-unsigned.apk
```

### Запуск тестов
```bash
# Запуск полного набора модульных и интеграционных тестов
./gradlew test
```

---

## Лицензия
Проект распространяется под лицензией [MIT](LICENSE).
Используемые бинарные компоненты FFmpeg лицензируются в соответствии с LGPLv2.1+ / GPLv2+ используемой сборки FFmpeg.
