package doichin.auth

import doichin.auth.dto.App
import org.jooq.DSLContext
import org.jooq.TransactionalCallable
import org.junit.jupiter.api.BeforeAll
import io.mockk.coEvery
import kotlin.uuid.Uuid
import io.mockk.mockk
import io.mockk.slot
import org.jooq.Configuration

abstract class BaseTest {
    companion object {
        lateinit var dslContext: DSLContext
        lateinit var appState: AppState

        val authApp = App(Uuid.fromLongs(0,0), "Auth")

        @JvmStatic
        @BeforeAll
        fun setUpOnce() {
            dslContext = mockk()
            val mockConfiguration: Configuration = mockk(relaxed = true)
            val transactionCallableSlot = slot<TransactionalCallable<Any>>()
            coEvery {
                dslContext.transactionResult(capture(transactionCallableSlot))
            } answers {
                transactionCallableSlot.captured.run(mockConfiguration)
            }

            AppState.ENV_TYPE = EnvType.TEST
            AppState.authApp = authApp
            appState = AppState
        }
    }
}
