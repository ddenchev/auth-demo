package doichin.auth.services.permission

import doichin.auth.dto.CreatePermissionRequest
import doichin.auth.dto.Permission
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory

val createPermission  by lazy { CreatePermission() }

/**
 * Service for creating permissions within an application.
 *
 * @property dslContext The DSL context for database operations.
 * @property permissionRepository Repository for permission data access.
 */
class CreatePermission(
    private val dslContext: DSLContext = Database.dslContext,
    private val permissionRepository: PermissionRepository = PermissionRepository(),
) {
    companion object {
        private const val MAX_ACTION_LENGTH = 20
    }

    private val log = LoggerFactory.getLogger(CreatePermission::class.java)

    suspend operator fun invoke(req: CreatePermissionRequest): Permission {

        return withContext(Dispatchers.IO) {
            dslContext.transactionResult { configuration ->
                val ctx = DSL.using(configuration)

                // Retrieve information we need
                val existingPermissions = permissionRepository.list(ctx)
                validate(req, existingPermissions)

                permissionRepository.insertPermission(ctx, req)
            }
        }
    }

    private fun validate(
        createPermissionRequest: CreatePermissionRequest,
        existingPermissions: List<Permission>
    ) {

        if (createPermissionRequest.action.isBlank()) {
            throw ValidationException("Action name cannot be blank")
        }

        if (createPermissionRequest.action.length > MAX_ACTION_LENGTH) {
            throw ValidationException("Action name cannot be longer than $MAX_ACTION_LENGTH characters")
        }

        if (
            existingPermissions.any {existingPermission ->
                existingPermission.resourceId == createPermissionRequest.resourceId
                && existingPermission.action == createPermissionRequest.action
            }
        ) {
            throw ValidationException("Permission ${createPermissionRequest.resourceId}:${createPermissionRequest.action} already exists.")
        }
    }
}
