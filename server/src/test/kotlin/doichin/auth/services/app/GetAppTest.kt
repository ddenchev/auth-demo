package doichin.auth.services.app

import doichin.auth.BaseTest
import doichin.auth.dto.App
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.AppRepository
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@MockKExtension.CheckUnnecessaryStub
class GetAppTest : BaseTest() {
    private lateinit var appRepository: AppRepository
    private lateinit var getApp: GetApp

    private val appId = Uuid.random()

    @BeforeEach
    fun setup() {
        appRepository = mockk<AppRepository>()
        getApp = GetApp(dslContext, appRepository)
    }

    @Test
    fun `success - retrieves app`() = runTest {
        val expectedApp = App(appId, "test-app")

        every { appRepository.getById(any(), appId) } returns expectedApp

        val actualApp = getApp(appId)

        assertEquals(expectedApp, actualApp)
        verify { appRepository.getById(any(), appId) }
    }

    @Test
    fun `failure - app not found`() = runTest {
        every { appRepository.getById(any(), appId) } returns null

        assertThrows<NotFoundException> {
            getApp(appId)
        }

        verify { appRepository.getById(any(), appId) }
    }

    @Test
    fun `failure - database access error`() = runTest {
        every { appRepository.getById(any(), appId) } throws DataAccessException("")

        assertThrows<DataAccessException> {
            getApp(appId)
        }

        verify { appRepository.getById(any(), appId) }
    }
}
