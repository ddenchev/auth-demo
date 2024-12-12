import org.gradle.kotlin.dsl.kover
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask


plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.dotenv.gradle)
    alias(libs.plugins.kotlinx.kover)
}

allprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlin.uuid.ExperimentalUuidApi"
            )
        )
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencies {
    kover(project(":server"))
    kover(project(":client"))
    kover(project(":admin-client"))
    kover(project(":app-client"))
    kover(project(":shared"))
    kover(project(":authorization-plugin"))
}
