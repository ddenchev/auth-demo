package doichin.auth.repositories

import doichin.auth.dto.App
import org.jooq.DSLContext
import org.jooq.generated.tables.records.AppRecord
import kotlin.uuid.Uuid
import org.jooq.generated.tables.App.APP as APP_TABLE

class AppRepository {

    fun getById(ctx: DSLContext, appId: Uuid): App? {

        val record = ctx.select()
            .from(APP_TABLE)
            .where(APP_TABLE.ID.eq(appId))
            .fetchOneInto(AppRecord::class.java)

        return record?.toDto()
    }

    fun retrieveAppByName(ctx: DSLContext, appName: String): App? {

        val record = ctx.select()
            .from(APP_TABLE)
            .where(APP_TABLE.APP_NAME.eq(appName))
            .fetchOneInto(AppRecord::class.java)

        return record?.toDto()
    }

    fun list(ctx: DSLContext, offset: Long = 0, limit: Long = 20): List<App> {

        return ctx.select()
            .from(APP_TABLE)
            .limit(limit)
            .offset(offset * limit)
            .fetch()
            .into(AppRecord::class.java)
            .map { it.toDto() }
    }

    fun count(ctx: DSLContext): Long {
        return ctx.selectCount()
            .from(APP_TABLE)
            .fetchOneInto(Long::class.java)
            ?: 0L
    }

    fun insertApp(ctx: DSLContext, appId: Uuid, appName: String): App {
        return ctx
            .insertInto(APP_TABLE)
            .set(APP_TABLE.ID, appId)
            .set(APP_TABLE.APP_NAME, appName)
            .returning()
            .fetchOne()
            ?.toDto()
            ?: throw IllegalStateException()
    }

    private fun AppRecord.toDto(): App {
        return App(this.id, this.appName)
    }
}