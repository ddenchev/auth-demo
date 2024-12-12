package doichin.auth.routes

import doichin.auth.dto.AllocateResourceRequest
import doichin.auth.dto.CreateResourceRequest
import doichin.auth.plugins.authorization.authorize
import doichin.auth.plugins.authorization.callingUser
import doichin.auth.services.resource.allocateResource
import doichin.auth.services.resource.createResource
import doichin.auth.services.resource.getResource
import doichin.auth.services.resource.listResources
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

// Permission keys used for authorization
const val RESOURCE_CREATE = "AAAABA=="
const val RESOURCE_READ = "AAAABQ=="
const val RESOURCE_ALLOCATE = "AAAABg=="
const val RESOURCE_READ_ALLOCATED = "AAAABw=="

fun Application.resourceRoutes() {
    routing {
        authorize(RESOURCE_CREATE) {
            post("/resources") {
                val req = call.receive<CreateResourceRequest>()
                val resource = createResource(req)
                call.respond(HttpStatusCode.Created, resource)
            }
        }

        authorize(RESOURCE_READ) {
            get("/resources") {
                val (offset, limit) = getPaginationParam()
                call.respond(listResources(offset, limit))
            }
            get("/resources/{id}") {
                val resourceId = getPathUuid("id")
                call.respond(getResource(resourceId))
            }
        }

        authorize(RESOURCE_READ_ALLOCATED) {
            get("/app/resources") {
                val (offset, limit) = getPaginationParam()
                val appId = call.callingUser().appId
                call.respond(listResources(appId, offset, limit))
            }

            get("/app/resources/{id}") {
                val resourceId = getPathUuid("id")
                val appId = call.callingUser().appId
                call.respond(getResource(appId, resourceId))
            }
        }



        authorize(RESOURCE_ALLOCATE) {
            post("resources/allocate") {
                val req = call.receive<AllocateResourceRequest>()

                val resources = allocateResource(req)
                call.respond(HttpStatusCode.OK, resources)
            }
        }
    }
}