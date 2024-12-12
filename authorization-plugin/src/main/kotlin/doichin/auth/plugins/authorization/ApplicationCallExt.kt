package doichin.auth.plugins.authorization

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.nio.ByteBuffer
import java.util.Base64
import kotlin.uuid.Uuid

fun ApplicationCall.callingUser(): CallingUser {
    val principal = this.principal<JWTPrincipal>()
        ?: throw AuthenticationException("Authorized user not found in request")

    val userId = principal.payload.getClaim("uid").asString()
    val appId = principal.audience[0]
    val permissions = principal.payload.claims["prm"]?.asString()

    var permissionMask: Long? = null
    if (permissions != null) {
        val decodedBytes = Base64.getDecoder().decode(permissions)
        permissionMask = ByteBuffer.wrap(decodedBytes).long
    }

    return CallingUser(Uuid.parse(userId), Uuid.parse(appId), permissionMask)
}