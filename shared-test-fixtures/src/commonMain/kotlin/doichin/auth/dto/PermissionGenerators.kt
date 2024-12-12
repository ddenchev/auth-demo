package doichin.auth.dto

import kotlin.random.Random
import kotlin.uuid.Uuid

fun CreatePermissionRequest.Companion.generate(): CreatePermissionRequest {
    return CreatePermissionRequest(
        resourceId = Uuid.random(),
        action = "action${Random.nextInt()}",
        description = "description${Random.nextInt()}",
        id = Uuid.random()
    )
}

fun CreatePermissionRequest.Companion.generate(resource: Resource): CreatePermissionRequest {
    return CreatePermissionRequest(
        resourceId = Uuid.random(),
        action = "action${Random.nextInt()}",
        description = "description${Random.nextInt()}",
        id = Uuid.random()
    )
}

fun Permission.Companion.generate(req: CreatePermissionRequest): Permission {
    return Permission(
        id = req.id,
        resourceName = "resourceName${Random.nextInt()}",
        resourceId = req.resourceId,
        action = req.action,
        description = req.description,
        key = "key${Random.nextInt()}"
    )
}

fun Permission.Companion.generate(): Permission {
    return Permission(
        id = Uuid.random(),
        resourceName = "resourceName${Random.nextInt()}",
        resourceId = Uuid.random(),
        action = "action${Random.nextInt()}",
        description = "description${Random.nextInt()}",
        key = "key${Random.nextInt()}",
    )
}

fun Permission.Companion.generate(id: Uuid): Permission {
    return Permission(
        id = id,
        resourceName = "resourceName${Random.nextInt()}",
        resourceId = Uuid.random(),
        action = "action${Random.nextInt()}",
        description = "description${Random.nextInt()}",
        key = "key${Random.nextInt()}",
    )
}


fun Permission.Companion.generate(resource: Resource): Permission {
    return Permission(
        id = Uuid.random(),
        resourceName = "resourceName${Random.nextInt()}",
        resourceId = resource.id,
        action = "action${Random.nextInt()}",
        description = "description${Random.nextInt()}",
        key = "key${Random.nextInt()}",
    )
}