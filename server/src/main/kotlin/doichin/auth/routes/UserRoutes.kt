package doichin.auth.routes

import doichin.auth.AppState
import doichin.auth.dto.CreateUserRequest
import doichin.auth.plugins.authorization.authorize
import doichin.auth.plugins.authorization.callingUser
import doichin.auth.services.user.addUserRole
import doichin.auth.services.user.createUser
import doichin.auth.services.user.getUser
import doichin.auth.services.user.listUsers
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

// Permission keys used for authorization
const val USER_CREATE = "AAAADw=="
const val USER_READ = "AAAAEA=="
const val USER_MANAGE_ROLES = "AAAAEQ=="

fun Application.userRoutes() {
    routing {
        authorize(USER_CREATE) {
            post("/users") {
                val appId = call.callingUser().appId
                val req = call.receive<CreateUserRequest>()

                val user = createUser(appId, req)

                call.respond(HttpStatusCode.OK, user)
            }
        }
        authorize(USER_READ) {
            get("/users/{id}") {
                val appId = call.callingUser().appId
                val userId = getPathUuid("id")
                call.respond(getUser(appId, userId))
            }

            get("/users/") {
                val (offset, limit) = getPaginationParam()
                val appId = call.callingUser().appId

                val users = listUsers(appId, offset, limit)

                call.respond(users)
            }
        }

        authorize(USER_MANAGE_ROLES) {
            post("/user/{userId}/role/{roleId}") {
                val userId = getPathUuid("userId")
                val roleId = getPathUuid("roleId")
                val appId = call.callingUser().appId

                val roles = addUserRole(appId, userId, roleId)

                call.respond(roles)
            }
        }


        authorize(USER_CREATE, {callingUser ->
            callingUser.appId == AppState.authApp.id
        }) {
            post("/apps/{appId}/users") {
                val appId = getPathUuid("appId")
                val req = call.receive<CreateUserRequest>()

                val user = createUser(appId, req)

                call.respond(HttpStatusCode.OK, user)
            }
        }
        authorize(USER_READ, {callingUser ->
            callingUser.appId == AppState.authApp.id
        }) {
            get("/apps/{appId}/users/{id}") {
                val appId = getPathUuid("appId")
                val userId = getPathUuid("id")

                call.respond(getUser(appId, userId))
            }

            get("/apps/{appId}/users/") {
                val (offset, limit) = getPaginationParam()
                val appId = getPathUuid("appId")

                val users = listUsers(appId, offset, limit)

                call.respond(users)
            }
        }

        authorize(USER_MANAGE_ROLES, {callingUser ->
            callingUser.appId == AppState.authApp.id
        }) {
            post("/apps/{appId}/user/{userId}/role/{roleId}") {
                val appId = getPathUuid("appId")
                val userId = getPathUuid("userId")
                val roleId = getPathUuid("roleId")

                val roles = addUserRole(appId, userId, roleId)

                call.respond(roles)
            }
        }
    }
}