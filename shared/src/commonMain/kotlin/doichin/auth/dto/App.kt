package doichin.auth.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class App(@Contextual val id: Uuid, val appName: String)

@Serializable
data class CreateAppRequest(
    val appName: String,
    val adminUser: CreateUserRequest,
    @Contextual val id: Uuid = Uuid.random()
)
