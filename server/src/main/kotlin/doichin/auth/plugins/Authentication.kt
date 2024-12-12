package doichin.auth.plugins

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*


fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("auth-jwt") {
            val env = dotenv {
                directory = ".."
                filename = ".env"
            }

            verifier(JWT
                .require(Algorithm.HMAC256(env["JWT_SECRET"] ?: ""))
                .withIssuer(env["JWT_ISSUER"])
                .build())

            /**
             * NOTE:
             * Normally, applications should also verify audience above:
             * `.withAudience(...)`
             *
             * This application is odd however, in that it does not validate the audience
             * for the token. It will perform certain actions even if the current user
             * is authenticated to one of the apps it manages.
             *
             * This behavior allows a user to work with this service, without having to
             * log in separately.
             */
            validate { credential ->
                if (
                    credential.payload.getClaim("userId").asString() != ""
                    && credential.audience[0] != ""
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}
