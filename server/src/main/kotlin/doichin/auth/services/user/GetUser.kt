package doichin.auth.services.user

import doichin.auth.dto.User
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import kotlin.uuid.Uuid

val getUser by lazy { GetUser() }

class GetUser(
    private val dslContext: DSLContext = Database.dslContext,
    private val userRepository: UserRepository = UserRepository(),
) {

    suspend operator fun invoke(appId: Uuid, userId: Uuid): User {
        return withContext(Dispatchers.IO) {
            userRepository.getByIdAppId(dslContext, appId, userId)
                ?: throw NotFoundException()
        }
    }
}