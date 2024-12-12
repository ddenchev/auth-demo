package doichin.auth.services.role

import doichin.auth.BaseTest
import doichin.auth.dto.Permission
import doichin.auth.dto.Role
import doichin.auth.dto.generate
import doichin.auth.lib.NotFoundException
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.RoleRepository
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@MockKExtension.CheckUnnecessaryStub
class ListRolePermissionsTest : BaseTest() {

    private lateinit var roleRepository: RoleRepository
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var listRolePermissions: ListRolePermissions

    private val roleId = Uuid.random()
    private val appId = Uuid.random()

    @BeforeEach
    fun setUp() {
        roleRepository = mockk()
        permissionRepository = mockk()
        listRolePermissions = ListRolePermissions(authApp, dslContext, roleRepository, permissionRepository)
    }

    @Test
    fun `success - list role permissions for auth app user`() = runTest {
        val permissions = listOf(
            Permission.generate(),
            Permission.generate(),
        )
        val role = Role.generate(appId)

        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns role
        every { permissionRepository.retrieveRolePermissions(any(), role) } returns permissions

        val result = listRolePermissions(appId, roleId)

        assertEquals(permissions, result)

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify { permissionRepository.retrieveRolePermissions(any(), role) }
    }

    @Test
    fun `success - list role permissions for own app user`() = runTest {
        val permissions = listOf(
            Permission.generate(),
            Permission.generate(),
        )
        val role = Role.generate(appId)

        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns role
        every { permissionRepository.retrieveRolePermissions(any(), role) } returns permissions

        val result = listRolePermissions(appId, roleId)

        assertEquals(permissions, result)

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify { permissionRepository.retrieveRolePermissions(any(), role) }
    }

    @Test
    fun `failure - role not found`() = runTest {
        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns null

        assertThrows<NotFoundException> {
            listRolePermissions(appId, roleId)
        }

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify(exactly = 0) { permissionRepository.retrieveRolePermissions(any(), any()) }
    }

}
