package doichin.auth.services.role

import doichin.auth.dto.Permission
import doichin.auth.lib.NotFoundException
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import kotlin.uuid.Uuid

val setRolePermissions by lazy { SetRolePermissions() }

class SetRolePermissions(
    private val dslContext: DSLContext = Database.dslContext,
    private val roleRepository: RoleRepository = RoleRepository(),
    private val permissionRepository: PermissionRepository = PermissionRepository()
) {

    suspend operator fun invoke(appId: Uuid, roleId: Uuid, permissionIds: List<Uuid>): List<Permission> {

        return withContext(Dispatchers.IO) {
            dslContext.transactionResult { configuration ->
                val ctx = DSL.using(configuration)

                // Retrieve information we need for authorization
                val role = roleRepository.getByIdAppId(ctx, appId, roleId)
                    ?: throw NotFoundException("Role with id $roleId not found")

                // Make sure that the new permissions for this role are ones that have been configured for the app
                val appPermissions =  permissionRepository.listByAppId(ctx, appId)
                val newPermissions = permissionIds.map {
                    appPermissions.find { permission -> permission.id == it }
                        ?: throw ValidationException("Permission with id $it not found")
                }

                // Reset permissions
                roleRepository.deleteRolePermissionsByRoleId(ctx, roleId)
                roleRepository.insertRolePermissions(ctx, roleId, newPermissions)
                permissionRepository.retrieveRolePermissions(ctx, role)
            }
        }
    }
}
