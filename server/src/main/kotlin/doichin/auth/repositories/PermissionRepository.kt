package doichin.auth.repositories

import doichin.auth.dto.CreatePermissionRequest
import doichin.auth.dto.Permission
import doichin.auth.dto.Role
import doichin.auth.dto.User
import doichin.auth.plugins.authorization.PermissionKey
import org.jooq.DSLContext
import org.jooq.generated.Tables.APP_RESOURCE
import org.jooq.generated.Tables.APP_USER
import org.jooq.generated.Tables.APP_USER_ROLE
import org.jooq.generated.Tables.PERMISSION
import org.jooq.generated.Tables.RESOURCE
import org.jooq.generated.Tables.ROLE_PERMISSION
import org.jooq.generated.tables.records.PermissionRecord
import org.jooq.generated.tables.records.ResourceRecord
import kotlin.uuid.Uuid

class PermissionRepository {

    fun insertPermission(ctx: DSLContext, createPermissionRequest: CreatePermissionRequest): Permission {
        val permissionRecord = ctx
            .insertInto(PERMISSION)
            .set(PERMISSION.ID, createPermissionRequest.id)
            .set(PERMISSION.RESOURCE_ID, createPermissionRequest.resourceId)
            .set(PERMISSION.ACTION, createPermissionRequest.action)
            .returning()
            .fetchOne()
            ?: throw IllegalStateException()

        val resourceRecord = ctx
            .select()
            .from(RESOURCE)
            .where(RESOURCE.ID.eq(permissionRecord.resourceId))
            .fetchOne()
            ?.into(ResourceRecord::class.java)
            ?: throw IllegalStateException()

        return Pair(permissionRecord, resourceRecord).toDto()
    }

    fun list(ctx: DSLContext, offset: Long = 0, limit: Long = 20): List<Permission> {
        return ctx
            .select()
            .from(PERMISSION)
            .innerJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
            .orderBy(PERMISSION.KEY.asc())
            .limit(limit)
            .offset(offset)
            .fetch { record ->
                val permission = record.into(PERMISSION).into(PermissionRecord::class.java)
                val resource = record.into(RESOURCE).into(ResourceRecord::class.java)
                Pair(permission, resource).toDto()
            }
    }

    fun count(ctx: DSLContext): Long {
        return ctx.selectCount()
            .from(PERMISSION)
            .fetchOneInto(Long::class.java)
            ?: 0L
    }

    fun listByResourceId(ctx: DSLContext, resourceId: Uuid, offset: Long = 0, limit: Long = 20): List<Permission> {
        println(resourceId)
        return ctx
            .select()
            .from(PERMISSION)
            .innerJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
            .where(RESOURCE.ID.eq(resourceId))
            .orderBy(PERMISSION.KEY.asc())
            .offset(offset)
            .limit(limit)
            .fetch { record ->
                val permission = record.into(PERMISSION).into(PermissionRecord::class.java)
                val resource = record.into(RESOURCE).into(ResourceRecord::class.java)
                Pair(permission, resource).toDto()
            }
    }

    fun listByAppId(ctx: DSLContext, appId: Uuid, offset: Long = 0, limit: Long = 20): List<Permission> {
        return ctx.select()
            .from(PERMISSION)
            .innerJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
            .innerJoin(APP_RESOURCE).on(APP_RESOURCE.RESOURCE_ID.eq(RESOURCE.ID))
            .where(APP_RESOURCE.APP_ID.eq(appId))
            .orderBy(PERMISSION.KEY.asc())
            .limit(limit)
            .offset(offset)
            .fetch { record ->
                val permission = record.into(PERMISSION).into(PermissionRecord::class.java)
                val resource = record.into(RESOURCE).into(ResourceRecord::class.java)
                Pair(permission, resource).toDto()
            }
    }

    fun countByAppId(ctx: DSLContext, appId: Uuid): Long {
        return ctx.selectCount()
            .from(PERMISSION)
            .innerJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
            .innerJoin(APP_RESOURCE).on(APP_RESOURCE.RESOURCE_ID.eq(RESOURCE.ID))
            .where(APP_RESOURCE.APP_ID.eq(appId))
            .fetchOneInto(Long::class.java)
            ?: 0L
    }

    fun retrieveRolePermissions(ctx: DSLContext, role: Role): List<Permission> {
        return ctx.select()
            .from(ROLE_PERMISSION)
            .innerJoin(PERMISSION).on(ROLE_PERMISSION.PERMISSION_ID.eq(PERMISSION.ID))
            .innerJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
            .where(ROLE_PERMISSION.ROLE_ID.eq(role.id))
            .fetch { record ->
                val permission = record.into(PERMISSION).into(PermissionRecord::class.java)
                val resource = record.into(RESOURCE).into(ResourceRecord::class.java)
                Pair(permission, resource).toDto()
            }
    }

    fun retrieveForUser(ctx: DSLContext, user: User): List<Permission> {
        return ctx.select()
            .from(APP_USER)
            .innerJoin(APP_USER_ROLE).on(APP_USER.ID.eq(APP_USER_ROLE.USER_ID))
            .innerJoin(ROLE_PERMISSION).on(APP_USER_ROLE.ROLE_ID.eq(ROLE_PERMISSION.ROLE_ID))
            .innerJoin(PERMISSION).on(ROLE_PERMISSION.PERMISSION_ID.eq(PERMISSION.ID))
            .innerJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
            .where(APP_USER.ID.eq(user.id))
            .fetch { record ->
                val permission = record.into(PERMISSION).into(PermissionRecord::class.java)
                val resource = record.into(RESOURCE).into(ResourceRecord::class.java)
                Pair(permission, resource).toDto()
            }
    }

    private fun Pair<PermissionRecord, ResourceRecord>.toDto(): Permission {
        return Permission(
            this.first.id,
            this.first.resourceId,
            this.second.name,
            this.first.action,
            this.first.description,
            PermissionKey(this.first.key).toString(),
        )
    }
}

