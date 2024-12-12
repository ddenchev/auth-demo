package doichin.auth.plugins.authorization

import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route

fun Route.authorize(
    permissionKey: String,
    authBlock: ((callingUser: CallingUser) -> Boolean)? = null,
    build: Route.() -> Unit
): Route {
    return authenticate(
        configurations = arrayOf("auth-jwt"),
        strategy = AuthenticationStrategy.FirstSuccessful
    ) {
        install(Authorization) {
            this.permissionKey = permissionKey
            this.authBlock = authBlock
        }

        build()
    }
}