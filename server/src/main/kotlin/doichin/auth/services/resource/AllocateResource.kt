package doichin.auth.services.resource

import doichin.auth.dto.AllocateResourceRequest
import doichin.auth.dto.Resource
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.ResourceRepository
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.db.suspendTransaction
import org.jooq.DSLContext

val allocateResource: AllocateResource by lazy { AllocateResource() }

class AllocateResource(
    private val dslContext: DSLContext = Database.dslContext,
    private val appRepository: AppRepository = AppRepository(),
    private val resourceRepository: ResourceRepository = ResourceRepository()
) {

    suspend operator fun invoke(req: AllocateResourceRequest): List<Resource> {
        return dslContext.suspendTransaction { ctx ->
            allocateResource(req, ctx)
        }
    }

    fun withContext(ctx: DSLContext): (AllocateResourceRequest) -> List<Resource> {
        return {allocateResourceRequest ->
            allocateResource(allocateResourceRequest, ctx)
        }
    }

    private fun allocateResource(req: AllocateResourceRequest, ctx: DSLContext): List<Resource> {
        val app = appRepository.getById(ctx, req.appId)
            ?: throw NotFoundException("App with id ${req.appId} not found")
        val resource = resourceRepository.getById(ctx, req.resourceId)
            ?: throw NotFoundException("Resource with id ${req.resourceId} not found")

        return resourceRepository.allocate(ctx, app, resource)
    }
}