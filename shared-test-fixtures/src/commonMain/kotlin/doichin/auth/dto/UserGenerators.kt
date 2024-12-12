package doichin.auth.dto

import kotlin.random.Random
import kotlin.uuid.Uuid

fun CreateUserRequest.Companion.generate(): CreateUserRequest {
    return CreateUserRequest(
        username = "user${Random.nextInt()}",
        email = "user@${Random.nextInt()}",
        firstName = "firstname${Random.nextInt()}",
        lastName = "lastname${Random.nextInt()}",
    )
}

fun User.Companion.generate(appId: Uuid, req: CreateUserRequest): User {
    return User(
        id = Uuid.random(),
        appId = appId,
        username = req.username,
        email = req.email,
        userStatus = UserStatus.PENDING,
        firstName = req.firstName,
        lastName = req.lastName,
    )
}

fun User.Companion.generate(appId: Uuid): User {
    return User(
        id = Uuid.random(),
        appId = appId,
        username = "user${Random.nextInt()}",
        email = "user@${Random.nextInt()}",
        userStatus = UserStatus.PENDING
    )
}