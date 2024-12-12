package doichin.auth.plugins

import doichin.auth.lib.AuthorizationException
import doichin.auth.lib.NotFoundException
import doichin.auth.lib.ServiceException
import doichin.auth.lib.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is ServiceException) {
                when (cause) {
                    is AuthorizationException -> {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            HttpErrorResponse(
                                HttpStatusCode.Forbidden.toString(),
                                cause.message ?: "Forbidden"
                            )
                        )
                    }
                    is ValidationException -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            HttpErrorResponse(
                                HttpStatusCode.BadRequest.toString(),
                                cause.message ?: "Bad Request"
                            )
                        )
                    }
                    is NotFoundException -> {
                        call.respond(
                            HttpStatusCode.NotFound,
                            HttpErrorResponse(
                                HttpStatusCode.NotFound.toString(),
                                cause.message ?: "Not Found"
                            )
                        )
                    }
                }
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    HttpErrorResponse(
                        HttpStatusCode.InternalServerError.toString(),
                        cause.message ?: "Internal Server Error"
                    )
                )
                throw cause
            }
        }
    }
}

@Serializable
data class HttpErrorResponse(val httpStatusCode: String, val message: String)

