package doichin.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class Listing<T>(
    val items: List<T>,
    val offset: Long,
    val limit: Long,
    val total: Long
)
