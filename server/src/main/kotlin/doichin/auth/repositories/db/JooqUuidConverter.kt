package doichin.auth.repositories.db

import org.jooq.impl.AbstractConverter
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class JooqUuidConverter: AbstractConverter<UUID, Uuid>(
    UUID::class.java,
    Uuid::class.java
) {

    override fun from(id: UUID): Uuid {
        return id.toKotlinUuid()
    }

    override fun to(id: Uuid): UUID {
        return id.toJavaUuid()
    }
}