package doichin.auth.services.app

import doichin.auth.BaseTest
import doichin.auth.dto.App
import doichin.auth.dto.Listing
import doichin.auth.repositories.AppRepository
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@MockKExtension.CheckUnnecessaryStub
class ListAppsTest: BaseTest() {

    private lateinit var appRepository: AppRepository
    private lateinit var listApps: ListApps

    @BeforeEach
    fun setup() {
        appRepository = mockk<AppRepository>()
        listApps = ListApps(dslContext, appRepository)
    }

    @Test
    fun `success - lists all apps`() = runTest {
        val app1 = App(Uuid.random(), "app 1")
        val app2 = App(Uuid.random(), "app 2")
        val items = listOf<App>(app1, app2)
        val (offset, limit) = Pair(0L, 10L)
        val expected = Listing(items, offset, limit, items.count().toLong())

        every { appRepository.list(any(), offset, limit) } returns items
        every { appRepository.count(any()) } returns items.count().toLong()

        val actual = listApps(offset, limit)
        assertEquals(actual, expected)
    }

    @Test
    fun `failure - database access error`() = runTest {
        val dataAccessException = DataAccessException("Access Error")

        every { appRepository.list(any(), any(), any()) } throws dataAccessException

        assertThrows<DataAccessException> {
            listApps(0, 10)
        }
    }
}