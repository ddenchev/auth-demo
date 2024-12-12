package doichin.auth.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.server.application.*

fun Application.configureSwaggerUI() {
    install(SwaggerUI) {
        info {
            title = "Auth API"
            version = "latest"
            description = "Auth Service API documentation."
        }
        server {
            url = "http://localhost:8080"
            description = "Development Server"
        }
    }
}