package doichin.auth.services.role

import doichin.auth.BaseTest
import doichin.auth.dto.Permission
import doichin.auth.dto.Resource
import doichin.auth.dto.Role
import doichin.auth.dto.generate
import doichin.auth.lib.NotFoundException
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.RoleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class SetRolePermissionsTest : BaseTest() {

    private lateinit var roleRepository: RoleRepository
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var setRolePermissions: SetRolePermissions

    private val roleId = Uuid.random()
    private val appId = Uuid.random()
    private val testResource = Resource.generate()

    @BeforeEach
    fun setUp() {
        roleRepository = mockk()
        permissionRepository = mockk()
        setRolePermissions = SetRolePermissions(dslContext, roleRepository, permissionRepository)
    }

    @Test
    fun `success - set role permissions for auth app user`() = runTest {
        val role = Role.generate(appId)
        val appPermissions = listOf(
            Permission.generate(),
            Permission.generate()
        )
        val permissionIds = appPermissions.map { it.id }
        val expectedPermissions = appPermissions

        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns role
        every { permissionRepository.listByAppId(any(), appId) } returns appPermissions
        every { roleRepository.deleteRolePermissionsByRoleId(any(), roleId) } returns Unit
        every { roleRepository.insertRolePermissions(any(), roleId, appPermissions) } returns Unit
        every { permissionRepository.retrieveRolePermissions(any(), role) } returns expectedPermissions

        val result = setRolePermissions(appId, roleId, permissionIds)

        assertEquals(expectedPermissions, result)

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify { permissionRepository.listByAppId(any(), appId) }
        verify { roleRepository.deleteRolePermissionsByRoleId(any(), roleId) }
        verify { roleRepository.insertRolePermissions(any(), roleId, appPermissions) }
        verify { permissionRepository.retrieveRolePermissions(any(), role) }
    }

    @Test
    fun `success - set role permissions for own app user`() = runTest {
        val role = Role.generate(appId)
        val appPermissions = listOf(
            Permission.generate(),
            Permission.generate()
        )
        val permissionIds = appPermissions.map { it.id }
        val expectedPermissions = appPermissions

        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns role
        every { permissionRepository.listByAppId(any(), appId) } returns appPermissions
        every { roleRepository.deleteRolePermissionsByRoleId(any(), roleId) } returns Unit
        every { roleRepository.insertRolePermissions(any(), roleId, appPermissions) } returns Unit
        every { permissionRepository.retrieveRolePermissions(any(), role) } returns expectedPermissions

        val result = setRolePermissions(appId, roleId, permissionIds)

        assertEquals(expectedPermissions, result)

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify { permissionRepository.listByAppId(any(), appId) }
        verify { roleRepository.deleteRolePermissionsByRoleId(any(), roleId) }
        verify { roleRepository.insertRolePermissions(any(), roleId, appPermissions) }
        verify { permissionRepository.retrieveRolePermissions(any(), role) }
    }

    @Test
    fun `failure - role not found`() = runTest {
        val permissionIds = listOf(Uuid.random())

        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns null

        assertThrows<NotFoundException> {
            setRolePermissions(appId, roleId, permissionIds)
        }

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify(exactly = 0) { permissionRepository.listByAppId(any(), any()) }
        verify(exactly = 0) { roleRepository.deleteRolePermissionsByRoleId(any(), any()) }
        verify(exactly = 0) { roleRepository.insertRolePermissions(any(), any(), any()) }
        verify(exactly = 0) { permissionRepository.retrieveRolePermissions(any(), any()) }
    }

    @Test
    fun `failure - invalid permission ids`() = runTest {
        val validPermissionId = Uuid.random()
        val invalidPermissionId = Uuid.random()
        val permissionIds = listOf(validPermissionId, invalidPermissionId)

        val role = Role(roleId, appId, "Test Role", "Description")
        val appPermissions = listOf(
            Permission.generate(validPermissionId),
        )

        every { roleRepository.getByIdAppId(any(), appId, roleId) } returns role
        every { permissionRepository.listByAppId(any(), appId) } returns appPermissions

        assertThrows<ValidationException> {
            setRolePermissions(appId, roleId, permissionIds)
        }

        verify { roleRepository.getByIdAppId(any(), appId, roleId) }
        verify { permissionRepository.listByAppId(any(), appId) }
        verify(exactly = 0) { roleRepository.deleteRolePermissionsByRoleId(any(), any()) }
        verify(exactly = 0) { roleRepository.insertRolePermissions(any(), any(), any()) }
        verify(exactly = 0) { permissionRepository.retrieveRolePermissions(any(), any()) }
    }
}
