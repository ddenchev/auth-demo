package doichin.auth.plugins.authorization

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.respond

class AuthorizationPluginConfig {
    lateinit var permissionKey: String
    var authBlock: ((callingUser: CallingUser) -> Boolean)? = null
}

val Authorization = createRouteScopedPlugin(
    name = "Authorization",
    createConfiguration = ::AuthorizationPluginConfig
) {
    val permissionKey = pluginConfig.permissionKey
    val authBlock = pluginConfig.authBlock

    on(AuthenticationChecked) { call ->
        try {
            val callingUser = call.callingUser()
            if (!callingUser.hasPermission(permissionKey)) {
                call.respond(HttpStatusCode.Forbidden, "User is not authorized to perform $permissionKey")
                return@on
            }

            authBlock?.invoke(callingUser)
        } catch (_: AuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, "User not found in request")
        }


    }
}

