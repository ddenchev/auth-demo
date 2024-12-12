package doichin.auth.services.app

import doichin.auth.dto.App
import doichin.auth.dto.Listing
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext

val listApps by lazy { ListApps() }

class ListApps(
    private val dslContext: DSLContext = Database.dslContext,
    private val appRepository: AppRepository = AppRepository()
) {

    suspend operator fun invoke(offset: Long = 0, limit: Long = 20): Listing<App> {
        return withContext(Dispatchers.IO) {
            val apps = appRepository.list(dslContext, offset, limit)
            val total = appRepository.count(dslContext)

            Listing(apps, offset, limit, total)
        }
    }
}

