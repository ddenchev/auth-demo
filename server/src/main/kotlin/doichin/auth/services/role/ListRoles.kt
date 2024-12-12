package doichin.auth.services.role

import doichin.auth.dto.Listing
import doichin.auth.dto.Role
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.RoleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import kotlin.uuid.Uuid

val listRoles by lazy { ListRoles() }

class ListRoles(
    private val dslContext: DSLContext = Database.dslContext,
    private val roleRepository: RoleRepository = RoleRepository()
) {

    suspend operator fun invoke(
        appId: Uuid,
        offset: Long = 0,
        limit: Long = 20
    ): Listing<Role> {
        return withContext(Dispatchers.IO) {
            val roles = roleRepository.listByAppId(dslContext, appId, offset, limit)
            val roleCount = roleRepository.countByAppId(dslContext, appId)

            Listing(roles, offset, limit, roleCount)
        }
    }

}

