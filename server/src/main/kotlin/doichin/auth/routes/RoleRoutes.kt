package doichin.auth.routes

import doichin.auth.AppState
import doichin.auth.dto.CreateRoleRequest
import doichin.auth.plugins.authorization.authorize
import doichin.auth.plugins.authorization.callingUser
import doichin.auth.services.role.createRole
import doichin.auth.services.role.listRoles
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

// Permission keys used for authorization
const val ROLE_CREATE = "AAAACw=="
const val ROLE_READ = "AAAADA=="

fun Application.roleRoutes() {

    routing {
        authorize(ROLE_CREATE) {
            post("/roles") {
                val req = call.receive<CreateRoleRequest>()
                val appId = call.callingUser().appId

                call.respond(
                    HttpStatusCode.Created,
                    createRole(appId, req)
                )
            }
        }

        authorize(ROLE_READ) {
            get("/roles") {
                val (offset, limit) = getPaginationParam()
                val appId = call.callingUser().appId

                val roles = listRoles(appId, offset, limit)

                call.respond(roles)
            }
        }


        authorize(ROLE_CREATE, {callingUser ->
            callingUser.appId == AppState.authApp.id
        }) {
            post("/apps/{appId}/roles") {
                val req = call.receive<CreateRoleRequest>()
                val appId = getPathUuid("appId")

                call.respond(
                    HttpStatusCode.Created,
                    createRole(appId, req)
                )
            }
        }

        authorize(ROLE_READ, {callingUser ->
            callingUser.appId == AppState.authApp.id
        }) {
            get("/apps/{appId}/roles") {
                val (offset, limit) = getPaginationParam()
                val appId = getPathUuid("appId")

                val roles = listRoles(appId, offset, limit)

                call.respond(roles)
            }
        }
    }
}