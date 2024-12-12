package doichin.auth.routes

import doichin.auth.AppState
import doichin.auth.dto.RoleAddPermissionRequest
import doichin.auth.plugins.authorization.authorize
import doichin.auth.plugins.authorization.callingUser
import doichin.auth.services.role.listRolePermissions
import doichin.auth.services.role.setRolePermissions
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlin.uuid.Uuid

// Permission keys used for authorization
const val ROLE_MANAGE_PERMISSIONS = "AAAADQ=="

fun Application.rolePermissionRoutes() {

    routing {
        authorize(ROLE_MANAGE_PERMISSIONS, {callingUser ->
            callingUser.appId == AppState.authApp.id
        }) {
            get("apps/{appId}/roles/{roleId}/permissions") {
                val roleId = getPathUuid("roleId")
                val appId = getPathUuid("appId")
                val permissions = listRolePermissions(appId, roleId)
                call.respond(permissions)
            }

            put("apps/{appId}/roles/{roleId}/permissions") {
                val roleId = getPathUuid("roleId")
                val appId = getPathUuid("appId")
                val req = call.receive<RoleAddPermissionRequest>()

                val permissions = setRolePermissions(
                    appId,
                    roleId,
                    req.permissionIds.map { Uuid.parse(it)}
                )

                call.respond(permissions)
            }
        }

        authorize(ROLE_MANAGE_PERMISSIONS) {
            get("/roles/{roleId}/permissions") {
                val roleId = getPathUuid("roleId")
                val appId = call.callingUser().appId
                val permissions = listRolePermissions(appId, roleId)
                call.respond(permissions)
            }

            put("/roles/{roleId}/permissions") {
                val roleId = getPathUuid("roleId")
                val appId = call.callingUser().appId
                val req = call.receive<RoleAddPermissionRequest>()

                val permissions = setRolePermissions(
                    appId,
                    roleId,
                    req.permissionIds.map { Uuid.parse(it)}
                )

                call.respond(permissions)
            }
        }
    }
}