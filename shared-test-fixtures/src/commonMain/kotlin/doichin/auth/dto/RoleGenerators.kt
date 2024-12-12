package doichin.auth.dto

import kotlin.random.Random
import kotlin.uuid.Uuid

fun CreateRoleRequest.Companion.generate(): CreateRoleRequest {
    return CreateRoleRequest(
        name = "role${Random.nextInt()}",
        description = "role${Random.nextInt()}",
    )
}

fun Role.Companion.generate(appId: Uuid, req: CreateRoleRequest): Role {
    return Role(
        id = Uuid.random(),
        appId = appId,
        name = req.name,
        description = req.description,
    )
}

fun Role.Companion.generate(appId: Uuid): Role {
    return Role(
        id = Uuid.random(),
        appId = appId,
        name = "role${Random.nextInt()}",
        description = "role description ${Random.nextInt()}",
    )
}