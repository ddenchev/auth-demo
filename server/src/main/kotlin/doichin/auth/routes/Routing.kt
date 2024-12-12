package doichin.auth.routes

import doichin.auth.lib.ValidationException
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlin.uuid.Uuid

fun Application.configureRouting() {
    appRoutes()
    resourceRoutes()
    authRoutes()
    permissionRoutes()
    roleRoutes()
    rolePermissionRoutes()
    userRoutes()

    routing {

        // Create a route for the swagger-ui using the openapi-spec at "/api.json".
        // This route will not be included in the spec.
        route("swagger") {
            swaggerUI("/api.json")
        }
        // Create a route for the openapi-spec file.
        // This route will not be included in the spec.
        route("api.json") {
            openApiSpec()
        }

    }
}

fun RoutingContext.getPathUuid(pathParam: String): Uuid {
    val pathParamValue = call.parameters[pathParam]
        ?: throw ValidationException("Invalid $pathParam format")
    try {
        return Uuid.parse(pathParamValue)
    } catch (e: IllegalArgumentException) {
        throw ValidationException("Invalid $pathParam format")
    }
}

fun RoutingContext.getPathString(pathParam: String): String {
    return call.pathParameters[pathParam]
        ?: throw ValidationException("Invalid $pathParam format")
}

fun RoutingContext.getPaginationParam(): Pair<Long, Long> {
    val offset = call.request.queryParameters["offset"]?.toLong() ?: 0
    val limit = call.request.queryParameters["limit"]?.toLong() ?: 20
    return Pair(offset, limit)
}