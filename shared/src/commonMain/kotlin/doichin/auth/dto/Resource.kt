package doichin.auth.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

enum class AuthResource {
    APP,
    RESOURCE,
    PERMISSION,
    ROLE,
    USER;

    // Common identifier
    fun cid() = this.name.lowercase()
}

@Serializable
data class Resource(
    @Contextual val id: Uuid,
    val name: String,
    val description: String,
    val resourceKey: Int,
)

@Serializable
data class CreateResourceRequest(
    val name: String,
    val description: String,
    @Contextual val id: Uuid = Uuid.random()
)

@Serializable
data class AllocateResourceRequest(
    @Contextual val resourceId: Uuid,
    @Contextual val appId: Uuid,
)