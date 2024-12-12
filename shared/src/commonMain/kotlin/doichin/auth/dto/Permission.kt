package doichin.auth.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Permission(
    @Contextual val id: Uuid,
    @Contextual val resourceId: Uuid,
    val resourceName: String,
    val action: String,
    val description: String? = null,
    val key: String,
)

@Serializable
data class CreatePermissionRequest(
    @Contextual val resourceId: Uuid,
    val action: String,
    val description: String? = null,
    @Contextual val id: Uuid = Uuid.random(),
)
