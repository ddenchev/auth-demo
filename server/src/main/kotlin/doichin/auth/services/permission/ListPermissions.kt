package doichin.auth.services.permission

import doichin.auth.dto.Listing
import doichin.auth.dto.Permission
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext

val listPermissions by lazy { ListPermissions() }

class ListPermissions(
    private val dslContext: DSLContext = Database.dslContext,
    private val permissionRepository: PermissionRepository = PermissionRepository()
) {

    suspend operator fun invoke(
        offset: Long = 0,
        limit: Long = 20
    ): Listing<Permission> {
        return withContext(Dispatchers.IO) {
            val permissions = permissionRepository.list(dslContext, offset, limit)
            val total = permissionRepository.count(dslContext)

            Listing(permissions, offset, limit, total)
        }
    }
}

