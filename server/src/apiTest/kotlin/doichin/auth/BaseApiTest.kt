package doichin.auth

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import io.mockk.every
import io.mockk.spyk
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import org.junit.jupiter.api.BeforeAll
import kotlin.uuid.Uuid

abstract class BaseApiTest {
    lateinit var authClient: doichin.auth.client.AuthClient

    companion object {
        val dotenv: Dotenv = dotenv {
            directory = ".."
            filename = ".env"
        }

        private lateinit var application: TestApplication
        lateinit var client: HttpClient

        @BeforeAll
        @JvmStatic
        fun setup() {
            val testEnv = spyk(dotenv)
            every { testEnv["ENV_TYPE"] } returns "test"
            application = TestApplication {
                application {
                    module(testEnv)
                }
            }

            client = application.createClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        serializersModule = serializersModuleOf(Uuid.serializer())
                    })
                }
            }
        }
    }
}