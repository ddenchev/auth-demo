@file:Suppress("SameReturnValue", "SameReturnValue")

package doichin.auth.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

enum class UserStatus {
    PENDING,
    VERIFIED,
    DEACTIVATED
}

@Serializable
data class User(
    @Contextual val id: Uuid,
    @Contextual val appId: Uuid,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val userStatus: UserStatus,
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class SetUserPasswordRequest(
    val password: String,
) {

    // Override to prevent accidental disclosure in logs
    override fun toString(): String {
        return ""
    }
}


// TODO: Move this out of dto folder, it should not be a part of the API
data class UserCredentials(
    @Contextual val userId: Uuid,
    val passwordHash: String,
    val passwordSalt: String,
) {
    // Override to prevent accidental disclosure in logs
    override fun toString(): String {
        return ""
    }
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
) {
    override fun toString(): String {
        return username
    }
}

@Serializable
data class LoginResponse(
    val token: String,
)