package doichin.auth.services.role

import doichin.auth.dto.CreateRoleRequest
import doichin.auth.dto.Role
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import kotlin.uuid.Uuid

val createRole by lazy { CreateRole() }

class CreateRole(
    private val dslContext: DSLContext = Database.dslContext,
    private val roleRepository: RoleRepository = RoleRepository(),
) {

    suspend operator fun invoke(
        appId: Uuid,
        req: CreateRoleRequest
    ): Role {
        return withContext(Dispatchers.IO) {
            dslContext.transactionResult { configuration ->
                val ctx = DSL.using(configuration)
                createUserRole(ctx, appId, req)
            }
        }
    }

    fun createUserRole(ctx: DSLContext, appId: Uuid, req: CreateRoleRequest): Role {
        val existingRoles = roleRepository.listByAppId(ctx, appId)
        validate(req, existingRoles)

        return roleRepository.insertRole(ctx, appId, req)
    }

    private fun validate(createRoleRequest: CreateRoleRequest, existingRoles: List<Role>) {
        if (createRoleRequest.name.length > 20) {
            throw ValidationException("Role name cannot be longer than 20 characters")
        }

        if (createRoleRequest.name.isBlank()) {
            throw ValidationException("Role name cannot be blank")
        }

        existingRoles.forEach { role ->
            if (role.name == createRoleRequest.name)
                throw ValidationException("A role with $role name already exists.")
        }
    }
}
