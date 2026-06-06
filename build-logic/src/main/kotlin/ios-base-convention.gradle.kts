import utils.configureIos

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

configureIos {
    compilations.all {
        compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-opt-in=kotlin.experimental.ExperimentalNativeApi")
                freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}

kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlin.experimental.ExperimentalObjCName")
            optIn("kotlin.experimental.ExperimentalNativeApi")
            if (name.lowercase().contains("ios")) {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
