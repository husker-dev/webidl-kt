plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "com.huskerdev"
version = "1.0.0"

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xexplicit-backing-fields"
        )
    }
    jvm()
}