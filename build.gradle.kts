plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.android)
}

group = "com.huskerdev"
version = "1.0.0"

kotlin {
    compilerOptions.freeCompilerArgs = listOf("-Xexplicit-backing-fields")

    jvm()
    js {
        browser()
        nodejs()
    }

    android {
        namespace = group.toString()
        compileSdk {
            version = release(36)
        }
    }

    mingwX64()

    linuxX64()
    linuxArm64()

    macosX64()
    macosArm64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}