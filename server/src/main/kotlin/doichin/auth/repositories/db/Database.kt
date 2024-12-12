package doichin.auth.repositories.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import javax.sql.DataSource

object Database {
    lateinit var dslContext: DSLContext

    fun init(dataSource: DataSource) {
        val configuration = DefaultConfiguration().apply {
            set(dataSource)
            set(SQLDialect.POSTGRES)
        }
        dslContext = DSL.using(configuration)
    }
}

suspend fun <T> DSLContext.suspendTransaction(block: (DSLContext) -> T): T {
    return withContext(Dispatchers.IO) {
        try {
            this@suspendTransaction.transactionResult { configuration ->
                val ctx = DSL.using(configuration)
                block(ctx)
            }
        } catch (e: Exception) {
            // Unwrap the original exception if there is one and throw it.
            // Any exception thrown while inside a transaction block, will trigger a rollback
            // and the original exception will be wrapped in a rollback exception.
            if (e.cause != null) throw e.cause!! else throw e
        }
    }
}