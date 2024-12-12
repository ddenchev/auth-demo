package doichin.auth.routes

import doichin.auth.AppState
import doichin.auth.dto.LoginRequest
import doichin.auth.dto.SetUserPasswordRequest
import doichin.auth.plugins.authorization.callingUser
import doichin.auth.services.user.GetUser
import doichin.auth.services.user.LoginUser
import doichin.auth.services.user.setUserPassword
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.authRoutes() {
    routing {
        post("/apps/{appId}/login", {
            description = "Allows a user to login into the specified app."
            request {
                pathParameter<String>("appId") {
                    description = "The uuid of the application into which the user is logging in."
                    example("00000000-0000-0000-0000-000000000000") {
                        value = "00000000-0000-0000-0000-000000000000"
                        summary = "The uuid for the Auth app."
                    }
                }
                body<LoginRequest> {
                    example("LoginRequest") {
                        value = LoginRequest("username", "password")
                        description = "User credentials."
                    }
                }
            }
            response {
                // information about a "200 OK" response
                code(HttpStatusCode.OK) {
                    // a description of the response
                    description = "Successful authentication."
                    body<HashMap<String, String>> {
                        mediaTypes(ContentType.Application.Json)
                        description = "A valid JWT token in the `token` field."
                        required = true
                    }
                }
            }
        }) {
            val appId = getPathUuid("appId")
            val loginRequest = call.receive<LoginRequest>()

            val loginUser = LoginUser()
            val token = loginUser(appId, loginRequest)

            call.respond(hashMapOf("token" to token))
        }

        post("/apps/{appId}/signUp") {
            // TODO: Implement app configuration which determines if the each app allows self sign up
        }

        post("/password_reset/{passwordResetToken}") {
            val passwordResetToken = getPathString("passwordResetToken")
            val req = call.receive<SetUserPasswordRequest>()

            val user = setUserPassword(passwordResetToken, req)

            call.respond(HttpStatusCode.OK, user)
        }

        // TODO: Deprecate
        post("/apps/auth/users/login") {
            val loginRequest = call.receive<LoginRequest>()
            val appId = AppState.authApp.id

            val loginUser = LoginUser()
            val token = loginUser(appId, loginRequest)

            call.respond(hashMapOf("token" to token))
        }

        authenticate("auth-jwt") {
            get("/users/me") {
                val callingUser = call.callingUser()

                val getUser = GetUser()
                val user = getUser(callingUser.appId, callingUser.userId)

                call.respond(user)
            }
        }
    }
}