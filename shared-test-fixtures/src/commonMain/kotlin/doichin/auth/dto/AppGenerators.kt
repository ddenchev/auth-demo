package doichin.auth.dto

import kotlin.random.Random
import kotlin.uuid.Uuid

fun CreateAppRequest.Companion.generate(): CreateAppRequest {
    return CreateAppRequest(
        id = Uuid.random(),
        appName = "app${Random.nextInt()}",
        adminUser = CreateUserRequest.generate()
    )
}

fun App.Companion.generate(req: CreateAppRequest): App {
    return App(
        id = req.id,
        appName = req.appName,
    )
}
