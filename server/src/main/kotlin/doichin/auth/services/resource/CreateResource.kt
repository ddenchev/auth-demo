package doichin.auth.services.resource

import doichin.auth.dto.CreateResourceRequest
import doichin.auth.dto.Resource
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.ResourceRepository
import doichin.auth.repositories.db.Database
import doichin.auth.repositories.db.suspendTransaction
import org.jooq.DSLContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val createResource: CreateResource by lazy { CreateResource() }

class CreateResource(
    private val dslContext: DSLContext = Database.dslContext,
    private val resourceRepository: ResourceRepository = ResourceRepository()
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CreateResource::class.java)
    }

    suspend operator fun invoke(req: CreateResourceRequest): Resource {
        return dslContext.suspendTransaction { ctx ->
            createResource(ctx, req)
        }
    }

    fun withContext(ctx: DSLContext): (CreateResourceRequest) -> Resource {
        return {createResourceRequest ->
            createResource(ctx, createResourceRequest)
        }
    }

    private fun createResource(ctx: DSLContext, req: CreateResourceRequest): Resource {
        val res = resourceRepository.getByName(ctx, req.name)
        validate(req, res)

        val resource = resourceRepository.insert(ctx, req)
        log.info("Created resource {}", req.name)

        return resource
    }

    private fun validate(req: CreateResourceRequest, existingResource: Resource?) {
        if (req.name.isEmpty()) throw ValidationException("Resource name cannot be empty")
        if (req.name.length > 20) throw ValidationException("Resource name cannot exceed 20 characters")
        if (existingResource != null) throw ValidationException("Resource '${req.name}' already exists")
    }
}