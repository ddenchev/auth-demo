package doichin.auth.services.app

import doichin.auth.dto.App
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import kotlin.uuid.Uuid


val getApp by lazy { GetApp() }

class GetApp(
    private val dslContext: DSLContext = Database.dslContext,
    private val appRepository: AppRepository = AppRepository()
) {

    suspend operator fun invoke(appId: Uuid): App {

        return withContext(Dispatchers.IO) {
            val app = appRepository.getById(dslContext, appId)
                ?: throw NotFoundException()
            app
        }
    }
}

