package doichin.auth.services.app

import doichin.auth.BaseTest
import doichin.auth.dto.*
import doichin.auth.dto.App
import doichin.auth.dto.CreateAppRequest
import doichin.auth.dto.CreateUserRequest
import doichin.auth.dto.User
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.ResourceRepository
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.UserRepository
import doichin.auth.services.role.CreateRole
import doichin.auth.services.user.CreateUser
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
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
class CreateAppTest: BaseTest() {
    private lateinit var appRepository: AppRepository
    private lateinit var createApp: CreateApp
    private lateinit var createUser: CreateUser
    private lateinit var createRole: CreateRole
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var userRepository: UserRepository
    private lateinit var resourceRepository: ResourceRepository

    @BeforeEach
    fun setup() {
        appRepository = mockk()
        createUser = mockk()
        createRole = mockk()
        permissionRepository = mockk(relaxed = true)
        roleRepository = mockk(relaxed = true)
        userRepository = mockk()
        resourceRepository = mockk(relaxed = true)

        createApp = CreateApp(
            dslContext,
            authApp,
            appRepository,
            permissionRepository,
            roleRepository,
            userRepository,
            resourceRepository,
            createUser,
            createRole
        )
    }

    @Test
    fun `success - creates app`() = runTest {
        val appRequest = CreateAppRequest.generate()
        val expectedApp = App.generate(appRequest)
        val adminUser = User.generate(appRequest.id, appRequest.adminUser)
        val createRoleRequest = CreateRoleRequest.generate()
        val adminRole = Role.generate(appRequest.id, createRoleRequest)

        every { appRepository.retrieveAppByName(any(), appRequest.appName) } returns null
        every { appRepository.insertApp(any(), any(), appRequest.appName) } returns expectedApp
        every {
            createUser.createUserRecords(any(), appRequest.id, appRequest.adminUser)
        } returns Pair(adminUser, "token")
        every { createRole.createUserRole(any(), any(), any())}  returns adminRole
        every { permissionRepository.insertPermission(any(), any())} returns mockk<Permission>()
        every { roleRepository.insertRolePermissions(any(), any(), any())} just Runs
        every { userRepository.upsertUserRole(any(), adminUser, adminRole)} returns listOf(adminRole)
        coEvery { createUser.sendUserVerificationEmail(any(), any())} just Runs

        val actualApp = createApp(appRequest)
        assertEquals(expectedApp, actualApp)

        verify(exactly = 1) {appRepository.insertApp(any(), any(), appRequest.appName) }
        verify(exactly = 1) {userRepository.upsertUserRole(any(), adminUser, adminRole) }
        coVerify(exactly = 1) {createUser.sendUserVerificationEmail(any(), any()) }
    }

    @Test
    fun `failure - app with a name that is too long`() = runTest {
        val appRequest = CreateAppRequest("a".repeat(21), CreateUserRequest.generate())

        every { appRepository.retrieveAppByName(any(), appRequest.appName) } returns null

        assertThrows<ValidationException> {
            createApp(appRequest)
        }

        verify(exactly = 0) { appRepository.insertApp(any(), any(), appRequest.appName) }
    }

    @Test
    fun `failure - app with this name already exists`() = runTest {
        val appRequest = CreateAppRequest("app", CreateUserRequest.generate())

        every { appRepository.retrieveAppByName(any(), appRequest.appName) } returns App(Uuid.random(), "test")

        assertThrows<ValidationException> {
            createApp(appRequest)
        }

        verify(exactly = 0) { appRepository.insertApp(any(), any(), appRequest.appName) }
    }


    @Test
    fun `failure - database access error`() = runTest {
        val appRequest = CreateAppRequest("app", CreateUserRequest.generate())

        every { appRepository.retrieveAppByName(any(), appRequest.appName) } throws DataAccessException("")

        assertThrows<DataAccessException> {
            createApp(appRequest)
        }

        verify(exactly = 0) { appRepository.insertApp(any(), any(), appRequest.appName) }
    }
}