package doichin.auth.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import doichin.auth.repositories.db.Database
import io.github.cdimascio.dotenv.dotenv

fun initDatabaseConnection() {
    val dataSource = createHikariDataSource()
    Database.init(dataSource)
}

fun createHikariDataSource(): HikariDataSource {
    val env = dotenv {
        directory = ".."
        filename = ".env"
    }

    val config = HikariConfig()
    config.driverClassName = "org.postgresql.Driver"
    config.jdbcUrl = env["POSTGRES_URL"]
    config.username = env["POSTGRES_USER"]
    config.password = env["POSTGRES_PASSWORD"]
    config.validate()

    return HikariDataSource(config)
}

