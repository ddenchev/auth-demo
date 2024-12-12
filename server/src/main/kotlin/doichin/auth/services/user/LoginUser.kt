package doichin.auth.services.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import doichin.auth.dto.LoginRequest
import doichin.auth.dto.Permission
import doichin.auth.dto.User
import doichin.auth.dto.UserStatus
import doichin.auth.lib.AuthorizationException
import doichin.auth.lib.verifyPassword
import doichin.auth.plugins.authorization.PermissionKey
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.Database
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.jooq.impl.DSL
import java.nio.ByteBuffer
import java.util.Base64
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.uuid.Uuid

class LoginUser (
    private val userRepository: UserRepository = UserRepository(),
    private val permissionRepository: PermissionRepository = PermissionRepository()
) {
    companion object {
        const val LOGIN_DURATION_MS = 60000000
    }

    suspend operator fun invoke(appId: Uuid, loginRequest: LoginRequest): String {
        return withContext(Dispatchers.IO) {
            try {
                Database.dslContext.transactionResult { configuration ->
                    val ctx = DSL.using(configuration)

                    val user = userRepository.retrieveUserByUsername(ctx, appId, loginRequest.username)
                        ?: throw AuthorizationException("Unknown user")

                    if (user.userStatus != UserStatus.VERIFIED) throw AuthorizationException("User is not in active")

                    val userCredentials = userRepository.retrieveUserCredentials(ctx, user.id)
                        ?: throw IllegalStateException("Unable to find user credentials")

                    if (!verifyPassword(loginRequest.password, userCredentials)) {
                        throw AuthorizationException("Incorrect password")
                    }

                    val permissions = permissionRepository.retrieveForUser(ctx, user)

                    generateJwtToken(user, permissions)
                }
            } catch (e: Exception) {
                // Unwrap the original exception if there is one and throw it
                if (e.cause != null) throw e.cause!! else throw e
            }
        }
    }

    private fun generateJwtToken(user: User, permissions: List<Permission>): String {
        val env = dotenv{
            directory = ".."
            filename = ".env"
        }

        val expirationTime = Clock.System.now()
            .plus(LOGIN_DURATION_MS.toDuration(DurationUnit.MILLISECONDS))

        // TODO: Consider adding jti claim, with a uuid for the token itself
        var jwt = JWT.create()
            .withIssuer(env["JWT_ISSUER"])
            .withAudience(user.appId.toString())
            .withExpiresAt(expirationTime.toJavaInstant())
            .withClaim("uid", user.id.toString())
            .withClaim("prm", encodeClaim(permissions))

        return jwt.sign(Algorithm.HMAC256(env["JWT_SECRET"]))
    }

    private fun encodeClaim(permissions: List<Permission>): String {
        var sig = 0L
        for (permission in permissions) {
            val key = PermissionKey.fromString(permission.key)
            sig = (1L shl key.value) or sig
        }

        return base64Encode(sig)
    }

    private fun base64Encode(value: Long): String {
        // Convert Long to ByteArray
        val byteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        byteBuffer.putLong(value)
        val byteArray = byteBuffer.array()

        // Encode ByteArray to Base64 String
        return Base64.getEncoder().encodeToString(byteArray)
    }
}