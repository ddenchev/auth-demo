package doichin.auth.services.user

import doichin.auth.dto.Listing
import doichin.auth.dto.User
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.db.suspendTransaction
import org.jooq.DSLContext
import kotlin.uuid.Uuid

val listUsers by lazy { ListUsers() }

class ListUsers(
    private val dslContext: DSLContext = Database.dslContext,
    private val userRepository: UserRepository = UserRepository(),
) {

    suspend operator fun invoke(
        appId: Uuid,
        offset: Long = 0,
        limit: Long = 20
    ): Listing<User> {
        return dslContext.suspendTransaction { ctx ->
            val users = userRepository.listByAppId(ctx, appId, offset, limit)
            val userCount = userRepository.countByAppId(ctx, appId)

            Listing(users, offset, limit, userCount)
        }
    }
}

