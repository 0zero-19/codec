import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-media"))
    implementation(project(":core-queue"))
    implementation(project(":core-profiles"))
    implementation(project(":core-settings"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "io.codec.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "Codec"
            packageVersion = "1.0.0"
            description = "Codec - Universal Media Converter & Compressor"
            copyright = "2026 Codec"
            vendor = "Codec"

            windows {
                menuGroup = "Codec"
                upgradeUuid = "a4176cf0-7814-41d6-b0df-d1264c1ea1a1"
                dirChooser = true
            }

            linux {
                shortcut = true
            }
        }
    }
}
