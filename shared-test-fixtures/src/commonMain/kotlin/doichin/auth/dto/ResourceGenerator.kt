package doichin.auth.dto

import kotlin.random.Random
import kotlin.uuid.Uuid


fun CreateResourceRequest.Companion.generate(): CreateResourceRequest {
    return CreateResourceRequest(
        name = "resource${Random.nextInt()}",
        description = "description${Random.nextInt()}",
    )
}

fun Resource.Companion.generate(req: CreateResourceRequest): Resource {
    return Resource(
        id = req.id,
        name = req.name,
        description = req.description,
        resourceKey = 1
    )
}

fun Resource.Companion.generate(): Resource {
    return Resource(
        id = Uuid.random(),
        name = "resource${Random.nextInt()}",
        description = "description${Random.nextInt()}",
        resourceKey = 1
    )
}

