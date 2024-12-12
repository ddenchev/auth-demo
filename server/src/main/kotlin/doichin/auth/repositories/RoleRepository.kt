package doichin.auth.repositories

import doichin.auth.dto.CreateRoleRequest
import doichin.auth.dto.Permission
import doichin.auth.dto.Role
import org.jooq.DSLContext
import org.jooq.generated.Tables.ROLE
import org.jooq.generated.Tables.ROLE_PERMISSION
import org.jooq.generated.tables.records.RoleRecord
import kotlin.uuid.Uuid

class RoleRepository {

    fun insertRole(ctx: DSLContext, appId: Uuid, createRoleRequest: CreateRoleRequest): Role {
        return ctx
            .insertInto(ROLE)
            .set(ROLE.NAME, createRoleRequest.name)
            .set(ROLE.DESCRIPTION, createRoleRequest.description)
            .set(ROLE.APP_ID, appId)
            .returning()
            .fetchOne()
            ?.toDto()
            ?: throw IllegalStateException()

    }

    fun getByIdAppId(ctx: DSLContext, appId: Uuid, roleId: Uuid): Role? {
        return ctx.select()
            .from(ROLE)
            .where(ROLE.ID.eq(roleId)
                .and(ROLE.APP_ID.eq(appId)))
            .fetchOneInto(RoleRecord::class.java)
            ?.toDto()
    }

    fun deleteRolePermissionsByRoleId(ctx: DSLContext, roleId: Uuid) {
        ctx.deleteFrom(ROLE_PERMISSION)
            .where(ROLE_PERMISSION.ROLE_ID.eq(roleId))
            .execute()
    }

    fun insertRolePermissions(ctx: DSLContext, roleId: Uuid, permissions: List<Permission>) {
        ctx.batch(
            permissions.map {
                ctx.insertInto(ROLE_PERMISSION)
                    .set(ROLE_PERMISSION.ROLE_ID, roleId)
                    .set(ROLE_PERMISSION.PERMISSION_ID, it.id)
            }
        ).execute()
    }

    fun grantPermissions(ctx: DSLContext, role: Role, permissions: List<Permission>) {
        ctx.batch(
            permissions.map {
                ctx.insertInto(ROLE_PERMISSION)
                    .set(ROLE_PERMISSION.ROLE_ID, role.id)
                    .set(ROLE_PERMISSION.PERMISSION_ID, it.id)
            }
        ).execute()
    }

    fun listByAppId(ctx: DSLContext, appId: Uuid, offset: Long = 0, limit: Long = 20): List<Role> {
        return ctx.select()
            .from(ROLE)
            .where(ROLE.APP_ID.eq(appId))
            .limit(limit)
            .offset(offset * limit)
            .fetch()
            .into(RoleRecord::class.java)
            .map { it.toDto() }
    }

    fun countByAppId(ctx: DSLContext, appId: Uuid): Long {
        return ctx.selectCount()
            .from(ROLE)
            .where(ROLE.APP_ID.eq(appId))
            .fetchOneInto(Long::class.java)
            ?: 0L
    }
}

fun RoleRecord.toDto(): Role {
    return Role(this.id, this.appId, this.name, this.description)
}
