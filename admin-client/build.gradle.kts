group = "doichin.auth"
version = "1.0.6"

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.gcp.artifactregistry)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.npm.publish)
}

kotlin {
    jvm()
    js(IR) {
        moduleName = "AuthAdminClient"
        browser()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":client"))

                // configuration
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutine.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.logback.classic)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}


publishing {
    publications {
        withType<MavenPublication> {
            groupId = project.group.toString()
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GoogleArtifactRegistry"
            url = uri("artifactregistry://us-east4-maven.pkg.dev/sercret-gold-fis-1552598193540/doichin-jvm")
        }
    }
}

npmPublish {
    registries {
        register("ArtifactRegistryNpm") {
            uri.set(uri("https://us-east4-npm.pkg.dev/sercret-gold-fis-1552598193540/doichin-js/"))
            authToken.set(providers.environmentVariable("NPM_TOKEN"))
        }
    }
    packages {
        version = project.version.toString()
    }
}