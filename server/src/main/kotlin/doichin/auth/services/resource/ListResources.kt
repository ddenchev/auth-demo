package doichin.auth.services.resource

import doichin.auth.dto.Listing
import doichin.auth.dto.Resource
import doichin.auth.repositories.ResourceRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import kotlin.uuid.Uuid

val listResources by lazy { ListResources() }

class ListResources(
    private val dslContext: DSLContext = Database.dslContext,
    private val resourceRepository: ResourceRepository = ResourceRepository(),
) {

    suspend operator fun invoke(
        appId: Uuid,
        offset: Long = 0,
        limit: Long = 20
    ): Listing<Resource> {
        return withContext(Dispatchers.IO) {
            val resources = resourceRepository.listByAppId(dslContext, appId, offset, limit)
            val total = resourceRepository.countByAppId(dslContext, appId)

            Listing(resources, offset, limit, total)
        }
    }

    suspend operator fun invoke(
        offset: Long = 0,
        limit: Long = 20
    ): Listing<Resource> {
        return withContext(Dispatchers.IO) {
            val resources = resourceRepository.list(dslContext, offset, limit)
            val total = resourceRepository.count(dslContext)

            Listing(resources, offset, limit, total)
        }
    }
}

