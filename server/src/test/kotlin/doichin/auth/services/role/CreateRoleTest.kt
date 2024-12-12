package doichin.auth.services.role

import doichin.auth.BaseTest
import doichin.auth.dto.CreateRoleRequest
import doichin.auth.dto.Role
import doichin.auth.dto.generate
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.RoleRepository
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
import kotlin.uuid.Uuid

@MockKExtension.CheckUnnecessaryStub
class CreateRoleTest: BaseTest() {
    private lateinit var roleRepository: RoleRepository
    private lateinit var createRole: CreateRole

    private val appId = Uuid.random()

    @BeforeEach
    fun setUp() {
        roleRepository = mockk<RoleRepository>()
        createRole = CreateRole(dslContext, roleRepository)
    }

    @Test
    fun `success - creates role using auth app`() = runTest {
        val req = CreateRoleRequest.generate()
        val expected = Role.generate(appId, req)

        every { roleRepository.listByAppId(any(), any()) } returns emptyList()
        every { roleRepository.insertRole(any(), appId, req) } returns expected

        val actual = createRole(appId, req)

        assertEquals(expected, actual)
    }

    @Test
    fun `success - creates role using other app`() = runTest {
        val req = CreateRoleRequest.generate()
        val expected = Role.generate(appId, req)

        every { roleRepository.listByAppId(any(), any()) } returns emptyList()
        every { roleRepository.insertRole(any(), appId, req) } returns expected

        val actual = createRole(appId, req)

        assertEquals(expected, actual)
    }

    @Test
    fun `failure - role name is blanc`() = runTest {
        val req = CreateRoleRequest("", "description")

        every { roleRepository.listByAppId(any(), any()) } returns emptyList()

        assertThrows<ValidationException> {
            createRole(appId, req)
        }
        verify(exactly = 0) { roleRepository.insertRole(any(), any(), any()) }
    }

    @Test
    fun `failure - role name is too long`() = runTest {
        val req = CreateRoleRequest("a".repeat(21), "description")

        every { roleRepository.listByAppId(any(), any()) } returns emptyList()

        assertThrows<ValidationException> {
            createRole(appId, req)
        }
        verify(exactly = 0) { roleRepository.insertRole(any(), any(), any()) }
    }

    @Test
    fun `failure - role already exists`() = runTest {
        val req = CreateRoleRequest.generate()
        val existingRole = Role.generate(appId, req)

        every { roleRepository.listByAppId(any(), any()) } returns listOf(existingRole)

        assertThrows<ValidationException> {
            createRole(appId, req)
        }
        verify(exactly = 0) { roleRepository.insertRole(any(), any(), any()) }
    }


    @Test
    fun `failure - data access error`() = runTest {
        val req = CreateRoleRequest.generate()

        every { roleRepository.listByAppId(any(), any()) } throws DataAccessException("")

        assertThrows<DataAccessException> {
            createRole(appId, req)
        }
        verify(exactly = 0) { roleRepository.insertRole(any(), any(), any()) }
    }
}