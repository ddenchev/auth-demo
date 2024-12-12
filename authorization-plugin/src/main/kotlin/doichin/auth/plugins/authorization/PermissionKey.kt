package doichin.auth.plugins.authorization

import java.nio.ByteBuffer
import java.util.Base64

@JvmInline
value class PermissionKey(val value: Int) {
    companion object {
        fun fromString(base64: String): PermissionKey {
            val decodedBytes = Base64.getDecoder().decode(base64)

            val byteBuffer = ByteBuffer.wrap(decodedBytes)
            return PermissionKey(byteBuffer.int)
        }
    }
    override fun toString(): String {
        val byteBuffer = ByteBuffer.allocate(Integer.BYTES)
        byteBuffer.putInt(value)
        val byteArray = byteBuffer.array()

        return Base64.getEncoder().encodeToString(byteArray)
    }

    fun toBitmask(): Long {
        return 1L shl value
    }
}