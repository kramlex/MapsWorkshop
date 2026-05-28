@file:Suppress("LocalVariableName")

import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.gradle.api.Action
import org.gradle.api.Task
import java.io.File
import java.io.Serializable

plugins {
    alias(libs.plugins.multiplatformModuleConvention)
    alias(libs.plugins.spmForKmp)
}

android.namespace = "ru.yandex.maps.workshop.mapkit.kmp"

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.swiftPackageConfig(cinteropName = "YMK") {
            minMacos = null
            minTvos = null
            minWatchos = null
            packageDependencyPrefix = "platform"
            minIos = "16.0"
            dependency {
                remoteBinary(
                    url = uri("https://github.com/c-villain/YandexMapsMobile/releases/download/4.17.0/YandexMapsMobile.xcframework.zip"),
                    packageName = "YandexMapsMobile",
                    exportToKotlin = true,
                    checksum = "f21284bc1a5f9cdd36aeeaf2eb1511bcb4e67c49e46d9561341351608d019459"
                )
            }

            exportedPackageSettings {
                name = "YMK"
            }
        }
    }

    sourceSets.androidMain.dependencies {
        api(libs.mapkit)
    }

    sourceSets.all {
        languageSettings {
            progressiveMode = true
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }
}

//region InteropHelper

val nativeInteropCinterop = layout.projectDirectory.dir("src/nativeInterop/cinterop")
val yandexMapsMobileDefTemplate = nativeInteropCinterop.file("YandexMapsMobile.def")
val spmGeneratedDefRoot = layout.buildDirectory.dir("spmKmpPlugin/YMK/defFiles")
val yandexMapsMobileDefTemplateFile = yandexMapsMobileDefTemplate.asFile
val spmGeneratedDefRootDir = spmGeneratedDefRoot.get().asFile

tasks.matching { it.name.startsWith("SwiftPackageConfigAppleYMKGenerateCInteropDefinition") }.configureEach {
    inputs.file(yandexMapsMobileDefTemplate)
    doLast(CopyYandexMapsMobileDefAction(yandexMapsMobileDefTemplateFile, spmGeneratedDefRootDir))
}

private class CopyYandexMapsMobileDefAction(
    private val yandexMapsMobileDefTemplate: File,
    private val spmGeneratedDefRootDir: File,
) : Action<Task>, Serializable {

    override fun execute(task: Task) {
        val compilerOptRegex = Regex("(?:^|\\s)(-[FI])\"([^\"]+)\"")

        spmGeneratedDefRootDir.walkTopDown()
            .filter { it.isFile && it.name == "YandexMapsMobile.def" }
            .forEach { generatedDef ->
                val defDirectory = generatedDef.parentFile
                val generatedCompilerOpts = generatedDef.readText()
                    .lineSequence()
                    .firstOrNull { it.startsWith("compilerOpts =") }
                    .orEmpty()
                val bridgeCompilerOpts = defDirectory.resolve("YMK_bridge.def")
                    .takeIf { it.isFile }
                    ?.readText()
                    ?.lineSequence()
                    ?.firstOrNull { it.startsWith("compilerOpts =") }
                    .orEmpty()

                val copiedDef = defDirectory.resolve("YandexMapsMobile.def")
                yandexMapsMobileDefTemplate.copyTo(copiedDef, overwrite = true)

                val compilerOpts = ("$generatedCompilerOpts $bridgeCompilerOpts")
                    .let { compilerOptRegex.findAll(it) }
                    .map { "${it.groupValues[1]}\"${it.groupValues[2]}\"" }
                    .distinct()
                    .joinToString(" ")

                val lines = copiedDef.readLines().map { line ->
                    if (line.startsWith("compilerOpts =")) {
                        "compilerOpts = $compilerOpts"
                    } else {
                        line
                    }
                }

                copiedDef.writeText(lines.joinToString(separator = "\n", postfix = "\n"))
            }
    }
}

//endregion
