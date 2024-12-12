package doichin.auth.services.resource

import doichin.auth.dto.Resource
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.ResourceRepository
import doichin.auth.repositories.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import kotlin.uuid.Uuid


val getResource by lazy { GetResource() }

class GetResource(
    private val dslContext: DSLContext = Database.dslContext,
    private val resourceRepository: ResourceRepository = ResourceRepository()
) {

    suspend operator fun invoke(appId: Uuid, resourceId: Uuid): Resource {
        return withContext(Dispatchers.IO) {
            val resource = resourceRepository.getByIdAppId(dslContext, appId, resourceId)
                ?: throw NotFoundException()
            resource
        }
    }

    suspend operator fun invoke(resourceId: Uuid): Resource {
        return withContext(Dispatchers.IO) {
            val resource = resourceRepository.getById(dslContext, resourceId)
                ?: throw NotFoundException()
            resource
        }
    }
}

