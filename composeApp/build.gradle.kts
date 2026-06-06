@file:Suppress("LocalVariableName")

import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

plugins {
    alias(libs.plugins.multiplatformAppConvention)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
}

"ru.yandex.maps.workshop".let { appId ->
    android.namespace = appId
    android.defaultConfig.applicationId = appId
    buildkonfig.packageName = appId
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}

fun secret(key: String, default: String = ""): String =
    localProperties.getProperty(key)
        ?: project.findProperty(key)?.toString()
        ?: default

buildkonfig {
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "mapkitToken", secret("mapkitToken"))
        buildConfigField(FieldSpec.Type.STRING, "openAiApiKey", secret("openAiApiKey"))
        buildConfigField(FieldSpec.Type.STRING, "openAiModel", secret("openAiModel", "deepseek-v4-flash"))
        buildConfigField(FieldSpec.Type.STRING, "openAiBaseUrl", secret("openAiBaseUrl", "https://api.deepseek.com/chat/completions"))
    }
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets.androidMain.dependencies {
        implementation(compose.preview)
        implementation(libs.androidx.activity.compose)
    }
    sourceSets.commonMain.dependencies {
        api(projects.common)

        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.materialIconsExtended)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.lifecycle.runtimeCompose)

    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
android {
    defaultConfig {
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}


