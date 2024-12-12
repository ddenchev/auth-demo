package doichin.auth.plugins.authorization

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CallingUser(
    @Contextual val userId: Uuid,
    @Contextual val appId: Uuid,
    val permissionMask: Long? = null,
) {
    fun hasPermission(key: String): Boolean {
        if (permissionMask == null) return false

        val bitMask = PermissionKey.fromString(key).toBitmask()

        return (permissionMask and bitMask) != 0L
    }
}