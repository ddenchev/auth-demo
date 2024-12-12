package doichin.auth.repositories

import doichin.auth.dto.App
import doichin.auth.dto.CreateResourceRequest
import doichin.auth.dto.Resource
import org.jooq.DSLContext
import org.jooq.generated.Tables.APP_RESOURCE
import org.jooq.generated.Tables.RESOURCE
import org.jooq.generated.tables.records.ResourceRecord
import kotlin.uuid.Uuid

class ResourceRepository {
    fun getById(ctx: DSLContext, id: Uuid): Resource? {
        return ctx.select()
            .from(RESOURCE)
            .where(RESOURCE.ID.eq(id))
            .fetchOneInto(ResourceRecord::class.java)
            ?.toDto()
    }

    fun getByIdAppId(ctx: DSLContext, id: Uuid, appId: Uuid): Resource? {
        return ctx.select()
            .from(RESOURCE)
            .innerJoin(APP_RESOURCE).on(RESOURCE.ID.eq(APP_RESOURCE.RESOURCE_ID))
            .where(RESOURCE.ID.eq(id))
            .and(APP_RESOURCE.APP_ID.eq(appId))
            .fetchOneInto(ResourceRecord::class.java)
            ?.toDto()
    }

    fun list(ctx: DSLContext, offset: Long, limit: Long): List<Resource> {
        return ctx
            .select()
            .from(RESOURCE)
            .limit(limit)
            .offset(offset)
            .fetch()
            .into(ResourceRecord::class.java)
            .map { it.toDto() }
    }

    fun listByAppId(ctx: DSLContext, appId: Uuid, offset: Long, limit: Long): List<Resource> {
        return ctx
            .select()
            .from(RESOURCE)
            .innerJoin(APP_RESOURCE).on(RESOURCE.ID.eq(APP_RESOURCE.RESOURCE_ID))
            .where(APP_RESOURCE.APP_ID.eq(appId))
            .limit(limit)
            .offset(offset)
            .fetch()
            .into(ResourceRecord::class.java)
            .map { it.toDto() }
    }

    fun count(ctx: DSLContext): Long {
        return ctx
            .selectCount()
            .from(RESOURCE)
            .fetchOne()
            ?.into(Long::class.java)
            ?: 0L
    }

    fun countByAppId(ctx: DSLContext, appId: Uuid): Long {
        return ctx
            .selectCount()
            .from(RESOURCE)
            .innerJoin(APP_RESOURCE).on(RESOURCE.ID.eq(APP_RESOURCE.RESOURCE_ID))
            .where(APP_RESOURCE.APP_ID.eq(appId))
            .fetchOne()
            ?.into(Long::class.java)
            ?: 0L
    }

    fun getByName(ctx: DSLContext, name: String): Resource? {
        return ctx.select()
            .from(RESOURCE)
            .where(RESOURCE.NAME.eq(name))
            .fetchOneInto(ResourceRecord::class.java)
            ?.toDto()

    }

    fun insert(ctx: DSLContext, req: CreateResourceRequest): Resource {
        return ctx
            .insertInto(RESOURCE)
            .set(RESOURCE.ID, req.id)
            .set(RESOURCE.NAME, req.name)
            .set(RESOURCE.DESCRIPTION, req.description)
            .returning()
            .fetchOne()
            ?.toDto()
            ?: throw IllegalStateException()
    }

    fun allocate(ctx: DSLContext, app: App, resource: Resource): List<Resource> {
        ctx
            .insertInto(APP_RESOURCE)
            .set(APP_RESOURCE.APP_ID, app.id)
            .set(APP_RESOURCE.RESOURCE_ID, resource.id)
            .execute()

        return ctx
            .select()
            .from(APP_RESOURCE)
            .innerJoin(RESOURCE).on(RESOURCE.ID.eq(APP_RESOURCE.RESOURCE_ID))
            .where(APP_RESOURCE.APP_ID.eq(app.id))
            .fetchInto(ResourceRecord::class.java)
            .map { it.toDto() }
    }
}

fun ResourceRecord.toDto(): Resource {
    return Resource(this.id, this.name, this.description, this.key)
}
