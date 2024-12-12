package doichin.auth.routes

import doichin.auth.dto.CreateAppRequest
import doichin.auth.plugins.authorization.*
import doichin.auth.services.app.createApp
import doichin.auth.services.app.getApp
import doichin.auth.services.app.listApps
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Permission keys used for authorization
const val APP_CREATE = "AAAAAQ=="
const val APP_READ = "AAAAAg=="

fun Application.appRoutes() {
    routing {
        authorize(APP_CREATE) {
            post("/apps") {
                val createAppRequest = call.receive<CreateAppRequest>()
                val app = createApp(createAppRequest)
                call.respond(HttpStatusCode.Created, app)
            }
        }

        authorize(APP_READ) {
            get("/apps") {
                val (offset, limit) = getPaginationParam()
                val apps = listApps(offset, limit)
                call.respond(apps)
            }

            get("/apps/{id}") {
                val appId = getPathUuid("id")
                val app = getApp(appId)
                call.respond(app)
            }
        }

    }
}