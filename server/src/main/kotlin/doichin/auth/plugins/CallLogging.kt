package doichin.auth.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level


fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}

