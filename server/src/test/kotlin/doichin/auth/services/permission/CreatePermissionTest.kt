package doichin.auth.services.permission

import doichin.auth.BaseTest
import doichin.auth.dto.CreatePermissionRequest
import doichin.auth.dto.Permission
import doichin.auth.dto.Resource
import doichin.auth.dto.generate
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.PermissionRepository
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

@MockKExtension.CheckUnnecessaryStub
class CreatePermissionTest: BaseTest() {
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var createPermission: CreatePermission

    private val testResource = Resource.generate()

    @BeforeEach
    fun setUp() {
        permissionRepository = mockk<PermissionRepository>()
        createPermission = CreatePermission(dslContext, permissionRepository)
    }

    @Test
    fun `success - creates permission using auth app`() = runTest {
        val req = CreatePermissionRequest.generate()
        val expected = Permission.generate(req)

        every { permissionRepository.list(any(), any()) } returns emptyList()
        every { permissionRepository.insertPermission(any(), req) } returns expected

        val actual = createPermission(req)

        assertEquals(expected, actual)
    }

    @Test
    fun `success - creates permission using other app`() = runTest {
        val req = CreatePermissionRequest.generate()
        val expected = Permission.generate(req)


        every { permissionRepository.list(any(), any()) } returns emptyList()
        every { permissionRepository.insertPermission(any(), req) } returns expected

        val actual = createPermission(req)

        assertEquals(expected, actual)
    }


    @Test
    fun `failure - action name is blanc`() = runTest {
        val req = CreatePermissionRequest(testResource.id, "", "description")

        every { permissionRepository.list(any(), any()) } returns emptyList()

        assertThrows<ValidationException> {
            createPermission(req)
        }
        verify(exactly = 0) { permissionRepository.insertPermission(any(), req) }
    }

    @Test
    fun `failure - action name is too long`() = runTest {
        val req = CreatePermissionRequest(testResource.id, "action".repeat(10), "description")

        every { permissionRepository.list(any(), any()) } returns emptyList()

        assertThrows<ValidationException> {
            createPermission(req)
        }
        verify(exactly = 0) { permissionRepository.insertPermission(any(), req) }
    }

    @Test
    fun `failure - permission already exists`() = runTest {
        val req = CreatePermissionRequest.generate(testResource)
        val existingPermission = Permission.generate(req)
        every { permissionRepository.list(any(), any()) } returns listOf(existingPermission)

        assertThrows<ValidationException> {
            createPermission(req)
        }
        verify(exactly = 0) { permissionRepository.insertPermission(any(), req) }
    }


    @Test
    fun `failure - data access error`() = runTest {
        val req = CreatePermissionRequest(testResource.id, "action".repeat(10), "description")

        every { permissionRepository.list(any(), any()) } throws DataAccessException("")

        assertThrows<DataAccessException> {
            createPermission(req)
        }
        verify(exactly = 0) { permissionRepository.insertPermission(any(), req) }
    }
}