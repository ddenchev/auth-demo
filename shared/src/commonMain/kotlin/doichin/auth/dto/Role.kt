package doichin.auth.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Role(
    @Contextual val id: Uuid,
    @Contextual val appId: Uuid,
    val name: String,
    val description: String,
)

@Serializable
data class CreateRoleRequest(
    val name: String,
    val description: String
)

@Serializable
data class RoleAddPermissionRequest(
    @Contextual val permissionIds: List<String>
)