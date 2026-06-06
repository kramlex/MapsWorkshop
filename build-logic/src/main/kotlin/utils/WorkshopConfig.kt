package utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val Project.iosEnabled: Boolean
    get() = providers.gradleProperty("workshop.enableIos").orNull?.toBooleanStrictOrNull() ?: false

val Project.includeIosX64: Boolean
    get() = providers.gradleProperty("workshop.iosX64").orNull?.toBooleanStrictOrNull() ?: false

fun Project.configureIos(configure: KotlinNativeTarget.() -> Unit = {}): List<KotlinNativeTarget> {
    if (!iosEnabled) return emptyList()
    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
    val targets = mutableListOf<KotlinNativeTarget>(
        kotlin.iosArm64(),
        kotlin.iosSimulatorArm64(),
    )
    if (includeIosX64) targets += kotlin.iosX64()
    targets.forEach(configure)
    return targets
}

fun Project.withIos(configure: KotlinMultiplatformExtension.() -> Unit) {
    if (!iosEnabled) return
    extensions.getByType(KotlinMultiplatformExtension::class.java).configure()
}
