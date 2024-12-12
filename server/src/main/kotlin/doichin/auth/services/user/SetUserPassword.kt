package doichin.auth.services.user

import doichin.auth.dto.SetUserPasswordRequest
import doichin.auth.dto.User
import doichin.auth.dto.UserCredentials
import doichin.auth.dto.UserStatus
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.impl.DSL
import doichin.auth.lib.*
import doichin.auth.lib.AuthorizationException
import doichin.auth.lib.NotFoundException
import doichin.auth.lib.ValidationException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.Uuid

data class PasswordResetRequest(
    val userId: Uuid,
    val token: String,
    val expiry: Instant
)

val setUserPassword by lazy { SetUserPassword() }

class SetUserPassword(
    private val userRepository: UserRepository = UserRepository(),
) {

    suspend operator fun invoke(passwordResetToken: String, req: SetUserPasswordRequest): User {
        validatePassword(req)
        val salt = generateRandom()
        val hash = generatePasswordHash(req.password, salt)

        return withContext(Dispatchers.IO) {
            try {
                Database.dslContext.transactionResult { configuration ->
                    val ctx = DSL.using(configuration)

                    val passwordResetRequest = validatePasswordResetRequest(
                        userRepository.getPasswordResetToken(ctx, passwordResetToken)
                    )
                    val user = userRepository.getById(ctx, passwordResetRequest.userId)
                        ?: throw NotFoundException("Unable to find user for this password reset token")
                    val userCredentials = UserCredentials(passwordResetRequest.userId, hash, salt)

                    val updatedUser = user.copy(userStatus = UserStatus.VERIFIED)
                    userRepository.upsertUserCredentials(ctx, userCredentials)
                    userRepository.updateUser(ctx, updatedUser)
                        ?: throw IllegalStateException()
                }
            } catch (e: Exception) {
                // Unwrap the original exception if there is one and throw it
                if (e.cause != null) throw e.cause!! else throw e
            }
        }
    }

    private fun validatePassword(req: SetUserPasswordRequest) {
        val lengthValid = req.password.length >= 12
        val specialCharacterValid = req.password.any {
            it.isLetterOrDigit().not()
        }
        val numberValid = req.password.any { it.isDigit() }

        if(!(lengthValid && specialCharacterValid && numberValid)) {
            throw ValidationException(
                "Password must be at least 12 character, " +
                "and contain at least 1 special character, and 1 number."
            )
        }
    }

    private fun validatePasswordResetRequest(passwordResetRequest: PasswordResetRequest?): PasswordResetRequest {
        if (passwordResetRequest == null) throw NotFoundException("Password reset token not found")
        if (passwordResetRequest.expiry < Clock.System.now()) throw AuthorizationException("Password reset token expired")
        return passwordResetRequest
    }

}