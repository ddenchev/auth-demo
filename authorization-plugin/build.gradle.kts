group = "doichin.auth"
version = "1.0.4"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gcp.artifactregistry)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}

publishing {
    publications {
        create<MavenPublication>("AuthorizationPlugin") {
            from(components["java"])
        }
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

