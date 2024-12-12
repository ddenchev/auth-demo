package doichin.auth.services.role

import doichin.auth.AppState
import doichin.auth.dto.App
import doichin.auth.dto.Permission
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import kotlin.uuid.Uuid

val listRolePermissions by lazy { ListRolePermissions() }

class ListRolePermissions(
    private val authApp: App = AppState.authApp,
    private val dslContext: DSLContext = Database.dslContext,
    private val roleRepository: RoleRepository = RoleRepository(),
    private val permissionRepository: PermissionRepository = PermissionRepository(),
) {

    suspend operator fun invoke(appId: Uuid, roleId: Uuid): List<Permission> {

        return withContext(Dispatchers.IO) {
            val role = roleRepository.getByIdAppId(dslContext, appId, roleId)
                ?: throw NotFoundException("Role with id $roleId not found")

            permissionRepository.retrieveRolePermissions(dslContext, role)
        }
    }
}

