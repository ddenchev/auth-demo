import nu.studer.gradle.jooq.JooqGenerate
import org.jooq.meta.jaxb.*

group = "doichin.auth"

repositories {
    mavenCentral()
}

plugins {
    application

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.jooq)
    alias(libs.plugins.flyway)
    alias(libs.plugins.kotlinx.kover)
}

buildscript {
    dependencies {
        classpath(libs.flyway.database.postgresql)
    }
}

sourceSets {
    // Add a new source set for API tests
    create("apiTest") {
        kotlin.srcDir("src/apiTest/kotlin")
        resources.srcDir("src/apiTest/resources")
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}


 val apiTestImplementation: Configuration by configurations.getting {
     extendsFrom(configurations.testImplementation.get())
 }

dependencies {
    implementation(project(":shared"))
    implementation(project(":authorization-plugin"))

    // Configuration
    implementation(libs.dotenv.kotlin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Ktor
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.swagger.ui)

    // Authentication
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)

    // Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.slf4j)
    implementation(libs.ktor.server.call.logging)

    // Postgres
    implementation(libs.postgresql)
    implementation(libs.jooq)
    implementation(libs.hikari)
    jooqGenerator(libs.postgresql)
    jooqGenerator(libs.logback.classic)


    // AWS
    implementation(libs.aws.ses.jvm)

    // Testing
    testImplementation(project(":shared-test-fixtures"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutine.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)

    apiTestImplementation(project(":client"))
    apiTestImplementation(project(":admin-client"))
    apiTestImplementation(project(":app-client"))
    apiTestImplementation(project(":shared"))
    apiTestImplementation(project(":shared-test-fixtures"))
    apiTestImplementation(libs.dotenv.kotlin)
    apiTestImplementation(libs.kotlinx.serialization.json)
    apiTestImplementation(libs.ktor.serialization.kotlinx.json)
    apiTestImplementation(libs.kotlinx.datetime)
}

application {
    mainClass.set("doichin.auth.ApplicationKt")
    val isDev: Boolean = env.ENV_TYPE.value == "dev"
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDev")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

jooq {
    version.set(libs.versions.jooq.lib.get())
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)

            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN

                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:15432/postgres"
                    user = env.POSTGRES_USER.value
                    password = env.POSTGRES_PASSWORD.value
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {


                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        forcedTypes.addAll(listOf(
                            ForcedType().apply {
                                userType = "kotlin.uuid.Uuid"
                                converter = "doichin.auth.repositories.db.JooqUuidConverter"
                                includeTypes = "UUID"
                            },
                            ForcedType().apply {
                                userType = "kotlinx.datetime.Instant"
                                converter = "doichin.auth.repositories.db.JooqDateTimeConverter"
                                includeTypes = "TIMESTAMP"
                            }
                        ))
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

flyway {
    driver = "org.postgresql.Driver"
    url = env.POSTGRES_URL.value
    user = env.POSTGRES_USER.value
    password = env.POSTGRES_PASSWORD.value
    schemas = arrayOf("public")
    cleanDisabled = env.ENV_TYPE.value == "prod"
}


tasks.named("flywayMigrate") {
    val markerFile = layout.buildDirectory.file("flyway/migrate.marker")

    doLast {
        markerFile.get().asFile.writeText("Migrations applied.")
    }

    outputs.file(markerFile)
}

tasks.named("flywayClean") {
    val markerFile = layout.buildDirectory.file("flyway/migrate.marker")

    doLast {
        // Check if the marker file exists before attempting to delete
        val file = markerFile.get().asFile
        if (file.exists()) {
            file.delete()
        }
    }
}

tasks.named<JooqGenerate>("generateJooq") {
    dependsOn("flywayMigrate")

    // Declare migration files as inputs
    inputs.files(fileTree("src/main/resources/db/migration"))
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // Declare other properties that affect code generation
    inputs.property("jooqVersion", jooq.version.get())
    inputs.property("flywayVersion", libs.versions.flyway.get())
    inputs.property("jooqConfig", jooq.configurations.toString())

    // Declare outputs
    outputs.dir(layout.buildDirectory.dir("generated-src/jooq/main"))
    allInputsDeclared = true
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.koverVerify)
}

tasks.register<Test>("apiTest") {
    description = "Runs the API tests."
    group = "verification"

    useJUnitPlatform()

    // Use the `apiTest` source set
    testClassesDirs = sourceSets["apiTest"].output.classesDirs
    classpath = sourceSets["apiTest"].runtimeClasspath
}