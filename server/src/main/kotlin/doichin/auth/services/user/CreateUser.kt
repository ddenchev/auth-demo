package doichin.auth.services.user

import doichin.auth.dto.CreateUserRequest
import doichin.auth.dto.User
import doichin.auth.lib.generateRandom
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.suspendTransaction
import doichin.auth.services.email.Email
import doichin.auth.services.email.SendEmail
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jooq.DSLContext
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.uuid.Uuid

val createUser by lazy { CreateUser() }

class CreateUser(
    private val dslContext: DSLContext = Database.dslContext,
    private val userRepository: UserRepository = UserRepository(),
    private val sendEmail: SendEmail = SendEmail()
) {

    suspend operator fun invoke(
        appId: Uuid,
        req: CreateUserRequest,
        dslContext: DSLContext = this.dslContext
    ): User {
        val (user, token) = dslContext.suspendTransaction { ctx ->
            createUserRecords(ctx, appId, req)
        }

        sendUserVerificationEmail(user, token)

        return user
    }

    fun createUserRecords(ctx: DSLContext, appId: Uuid, req: CreateUserRequest): Pair<User, String> {
        validate(req)
        val passwordResetToken = generateRandom()
        val expiry = getExpiry()

        // Retrieve information we need
        val existingUser = userRepository.retrieveUserByUsername(ctx, appId, req.username)
        if (existingUser != null) throw ValidationException("Username already exists")

        val user = userRepository.insertUser(ctx, appId, req)
        val token = userRepository.insertPasswordResetToken(ctx, user.id, passwordResetToken, expiry)

        return Pair(user, token)
    }

    suspend fun sendUserVerificationEmail(user: User, passwordResetToken: String) {
        // TODO: Use message outboxing to make this function resilient
        sendEmail(Email(
            to = listOf(user.email),
            from = "validation@auth.doich.in",
            subject = "Please Validate Your Email",
            body = "Use the following [$passwordResetToken] to validate your email and set a password."
        ))
    }

    private fun validate(req: CreateUserRequest) {
        if (req.username.length > 40) {
            throw ValidationException("Username cannot be longer than 40 characters")
        }
    }

    private fun getExpiry(): Instant {
        val now = Clock.System.now()
        return now.plus(1.toDuration(DurationUnit.HOURS))
    }
}
