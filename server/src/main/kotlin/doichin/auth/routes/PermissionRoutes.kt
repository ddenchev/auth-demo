package doichin.auth.routes

import doichin.auth.dto.CreatePermissionRequest
import doichin.auth.plugins.authorization.authorize
import doichin.auth.services.permission.createPermission
import doichin.auth.services.permission.listPermissions
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

// Permission keys used for authorization
const val PERMISSION_CREATE = "AAAACA=="
const val PERMISSION_READ = "AAAACQ=="

fun Application.permissionRoutes() {
    routing {
        authorize(PERMISSION_CREATE) {
            post("/permissions") {
                val req = call.receive<CreatePermissionRequest>()

                call.respond(
                    HttpStatusCode.Created,
                    createPermission(req)
                )
            }
        }

        authorize(PERMISSION_READ) {
            get("/permissions") {
                val (offset, limit) = getPaginationParam()
                val permissions = listPermissions(offset, limit)
                call.respond(permissions)
            }
        }
    }
}