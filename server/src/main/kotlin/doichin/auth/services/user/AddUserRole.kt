package doichin.auth.services.user

import doichin.auth.dto.Role
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.db.suspendTransaction
import org.jooq.DSLContext
import kotlin.uuid.Uuid

val addUserRole by lazy { AddUserRole() }

class AddUserRole(
    private val dslContext: DSLContext = Database.dslContext,
    private val userRepository: UserRepository = UserRepository(),
    private val roleRepository: RoleRepository = RoleRepository(),
) {

    suspend operator fun invoke(appId: Uuid, userId: Uuid, roleId: Uuid): List<Role> {
        return dslContext.suspendTransaction {ctx ->
            /* NOTE:
                These lookups are implicit authorization checks. If the user or the role
                do not belong to correct app context, the operation will not be executed.
             */
            val user = userRepository.getByIdAppId(ctx, appId, userId)
                ?: throw NotFoundException()
            val role = roleRepository.getByIdAppId(ctx, appId, roleId)
                ?: throw NotFoundException()

            userRepository.upsertUserRole(ctx, user, role)
        }
    }
}