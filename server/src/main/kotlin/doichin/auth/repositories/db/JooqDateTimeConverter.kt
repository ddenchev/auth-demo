package doichin.auth.repositories.db

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import org.jooq.impl.AbstractConverter
import java.time.LocalDateTime
import java.time.ZoneId

/*
* This converter assumes that timestamps stored in the database are all
* stored using UTC timezone and the system executing this application
* is also configured to use UTC timezone as its default.
* */
class JooqDateTimeConverter: AbstractConverter<LocalDateTime, Instant>(
    LocalDateTime::class.java,
    Instant::class.java
) {

    override fun from(colVal: LocalDateTime): Instant {
        return colVal
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toKotlinInstant()
    }

    override fun to(instant: Instant): LocalDateTime {
        return instant
            .toLocalDateTime(TimeZone.UTC)
            .toJavaLocalDateTime()
    }
}