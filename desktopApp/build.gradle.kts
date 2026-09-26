import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    // Keep emitted bytecode compatible with the Android project's Java 17 baseline.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "com.music.bitchord.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "BitChord"
            packageVersion = "1.0.0"
            description = "BitChord desktop preview"
            vendor = "BitChord"
        }
    }
}
