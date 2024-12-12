package doichin.auth

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.ses.SesClient
import doichin.auth.dto.App
import doichin.auth.plugins.*
import doichin.auth.routes.configureRouting
import doichin.auth.services.app.initializeAuthApp
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext

fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutdown signal received. Stopping server...")
        runBlocking {
            server.stop(gracePeriodMillis = 500, timeoutMillis = 500)
        }
        println("Server stopped.")
    })

    // Keep the main thread alive to continue receiving requests
    Thread.currentThread().join()
}

fun Application.module(
    env: Dotenv = dotenv {
        directory = ".."
        filename = ".env"
    }
) {
    // Configure ktor
    configureContentNegotiation()
    configureCompression()
    configureAuthentication()
    configureStatusPages()
    configureCallLogging()
    configureCORS()
    configureSwaggerUI()
    configureRouting()

    // Initialize the app
    initDatabaseConnection()
    AppState.init(this, env)

    log.info("--> Application Initialized")
    log.info("--> ENVIRONMENT_TYPE: ${AppState.ENV_TYPE}")
    log.info("--> AUTH APP ID: ${AppState.authApp.id}")
}



object AppState {
    private lateinit var env: Dotenv

    lateinit var ENV_TYPE: EnvType
    lateinit var authApp: App
    lateinit var dslContext: DSLContext // TODO:
    lateinit var emailClient: SesClient

    var FORCE_DEV_EMAIL: Boolean = false

    fun init(app: Application, env: Dotenv) {
        app.log.info("--> Initializing app state...")

        app.log.info("--> Configuring environment...")
        initializeEnv(env, app)


        app.log.info("--> Initializing email client...")
        initializeEmailClient(env)

        app.log.info("--> Initializing auth app...")
        runBlocking {
            authApp = initializeAuthApp()
        }

        app.log.info("--> Installing lifecycle monitors...")
        app.monitor.subscribe(ApplicationStopped) { application ->
            app.log.info("--> Server is stopping...")

            app.log.info("--> Closing email client...")
            emailClient.close()
            app.log.info("--> Email client closed")
        }
    }

    private fun initializeEnv(env: Dotenv, app: Application) {
        this.env = env

        this.ENV_TYPE = EnvType.entries.firstOrNull { env["ENV_TYPE"] == it.env }
            ?: throw IllegalArgumentException("Unknown environment type ${env["ENV_TYPE"]}. " +
                    "Supported environment types are: ${EnvType.entries}")

        this.FORCE_DEV_EMAIL = env["FORCE_DEV_EMAIL"].toBoolean()

        if (this.ENV_TYPE == EnvType.DEV && this.FORCE_DEV_EMAIL) {
            app.log.warn("--> !!! FORCE_DEV_EMAIL set to true !!!")
        } else if (this.ENV_TYPE == EnvType.DEV && !this.FORCE_DEV_EMAIL) {
            app.log.warn("--> NOTE: In dev environment user sign up emails will not be sent.")
        }
    }

    private fun initializeEmailClient(env: Dotenv) {
        emailClient = SesClient {
            region = env["AWS_REGION"]
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = env["AWS_ACCESS_KEY_ID"]
                secretAccessKey = env["AWS_SECRET_ACCESS_KEY"]
            }
        }

    }

}



