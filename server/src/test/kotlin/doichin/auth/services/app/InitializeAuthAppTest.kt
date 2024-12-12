package doichin.auth.services.app

import doichin.auth.BaseTest
import doichin.auth.dto.AllocateResourceRequest
import doichin.auth.dto.App
import doichin.auth.dto.CreateResourceRequest
import doichin.auth.dto.Resource
import doichin.auth.dto.Role
import doichin.auth.dto.User
import doichin.auth.dto.UserCredentials
import doichin.auth.dto.UserStatus
import doichin.auth.dto.generate
import doichin.auth.lib.verifyPassword
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.UserRepository
import doichin.auth.services.resource.AllocateResource
import doichin.auth.services.resource.CreateResource
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@MockKExtension.CheckUnnecessaryStub
class InitializeAuthAppTest : BaseTest() {
    private lateinit var appRepository: AppRepository
    private lateinit var userRepository: UserRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var createResource: CreateResource
    private lateinit var allocateResource: AllocateResource
    private lateinit var initializeAuthApp: InitializeAuthApp

    @BeforeEach
    fun setup() {
        mockkStatic("doichin.auth.lib.AuthUtilKt")

        appRepository = mockk()
        userRepository = mockk()
        roleRepository = mockk()
        permissionRepository = mockk()
        createResource = mockk()
        allocateResource = mockk()

        initializeAuthApp = InitializeAuthApp(
            dslContext,
            appRepository,
            userRepository,
            roleRepository,
            permissionRepository,
            createResource,
            allocateResource
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `success - initializes auth app when it exists`() = runTest {
        val authApp = App(Uuid.random(), "Auth")
        val rootUser = User(Uuid.random(), authApp.id, "root", "", userStatus = UserStatus.PENDING)
        val verifiedRootUser = rootUser.copy(userStatus = UserStatus.VERIFIED)
        val userCredentials = UserCredentials(rootUser.id, "hashed_password", "salt")

        every { appRepository.retrieveAppByName(any(), "Auth") } returns authApp
        every { userRepository.retrieveUserByUsername(any(), authApp.id, "root") } returns rootUser
        every { userRepository.upsertUserCredentials(any(), any()) } returns mockk<UserCredentials>()
        every { userRepository.retrieveUserCredentials(any(), any())} returns userCredentials
        every { userRepository.updateUser(any(), any()) } returns verifiedRootUser
        every { verifyPassword(any(), any()) } returns true

        val resultApp = initializeAuthApp()

        assertEquals(authApp, resultApp)
    }

    @Test
    fun `success - initializes auth app when it does not exist`() = runTest {
        val authAppId = Uuid.fromLongs(0,0)
        val authApp = App(authAppId, "Auth")
        val rootUser = User(Uuid.random(), authAppId, "root", "", userStatus = UserStatus.PENDING)
        val verifiedRootUser = rootUser.copy(userStatus = UserStatus.VERIFIED)
        val userCredentials = UserCredentials(rootUser.id, "hashed_password", "salt")
        val adminRole = Role(Uuid.random(), authAppId, "Admin", "App administrator")
        val resourceReq = CreateResourceRequest.generate()
        val createResourceMockk = fun(req: CreateResourceRequest): Resource {
            return Resource.generate(resourceReq)
        }
        val allocateResourceMockk = fun (req: AllocateResourceRequest): List<Resource> {
            return listOf(Resource.generate(resourceReq))
        }

        every { appRepository.retrieveAppByName(any(), "Auth") } returns null
        every { appRepository.insertApp(any(), any(), "Auth") } returns authApp
        every { roleRepository.insertRole(any(), any(), any()) } returns adminRole
        every { permissionRepository.insertPermission(any(), any()) } returns mockk()
        every { userRepository.insertUser(any(), authAppId, any()) } returns rootUser
        every { userRepository.upsertUserCredentials(any(), any()) } returns userCredentials
        every { userRepository.updateUser(any(), any()) } returns verifiedRootUser
        every { roleRepository.insertRolePermissions(any(), adminRole.id, any()) } just Runs
        every { userRepository.upsertUserRole(any(), verifiedRootUser, adminRole) } returns listOf(adminRole)
        every { createResource.withContext(any()) } returns createResourceMockk
        every { allocateResource.withContext(any()) } returns allocateResourceMockk

        every { userRepository.retrieveUserByUsername(any(), authApp.id, "root") } returns rootUser
        every { userRepository.retrieveUserCredentials(any(), any())} returns userCredentials

        val resultApp = initializeAuthApp()

        assertEquals(authApp, resultApp)
    }


    @Test
    fun `failure - root user does not exist`() = runTest {
        val authApp = App(Uuid.random(), "Auth")

        every { appRepository.retrieveAppByName(any(), "Auth") } returns authApp
        every { userRepository.retrieveUserByUsername(any(), authApp.id, "root") } returns null

        assertThrows<IllegalStateException> {
            initializeAuthApp()
        }

        verifySequence {
            appRepository.retrieveAppByName(any(), "Auth")
            userRepository.retrieveUserByUsername(any(), authApp.id, "root")
        }
    }

    @Test
    fun `failure - root user credentials missing`() = runTest {
        val authApp = App(Uuid.random(), "Auth")
        val rootUser = User(Uuid.random(), authApp.id, "root", "", userStatus = UserStatus.VERIFIED)

        every { appRepository.retrieveAppByName(any(), "Auth") } returns authApp
        every { userRepository.retrieveUserByUsername(any(), authApp.id, "root") } returns rootUser
        every { userRepository.retrieveUserCredentials(any(), rootUser.id) } returns null

        assertThrows<IllegalStateException> {
            initializeAuthApp()
        }

        verifySequence {
            appRepository.retrieveAppByName(any(), "Auth")
            userRepository.retrieveUserByUsername(any(), authApp.id, "root")
            userRepository.retrieveUserCredentials(any(), rootUser.id)
        }
    }

    @Test
    fun `success - root user credentials updated when password changes`() = runTest {
        val authApp = App(Uuid.random(), "Auth")
        val rootUser = User(Uuid.random(), authApp.id, "root", "", userStatus = UserStatus.VERIFIED)
        val oldCredentials = UserCredentials(rootUser.id, "oldHash", "oldSalt")
        val newCredentials = UserCredentials(rootUser.id, "newHash", "newSalt")

        every { appRepository.retrieveAppByName(any(), "Auth") } returns authApp
        every { userRepository.retrieveUserByUsername(any(), authApp.id, "root") } returns rootUser
        every { userRepository.retrieveUserCredentials(any(), rootUser.id) } returns oldCredentials
        every { userRepository.upsertUserCredentials(any(), any()) } returns newCredentials
        every { userRepository.updateUser(any(), any()) } returns rootUser

        val resultApp = initializeAuthApp()

        assertEquals(authApp, resultApp)

        verifySequence {
            appRepository.retrieveAppByName(any(), "Auth")
            userRepository.retrieveUserByUsername(any(), authApp.id, "root")
            userRepository.retrieveUserCredentials(any(), rootUser.id)
            userRepository.upsertUserCredentials(any(), any())
            userRepository.updateUser(any(), any())
        }
    }

    @Test
    fun `failure - database access error`() = runTest {
        val dataAccessException = DataAccessException("Access Error")
        every { appRepository.retrieveAppByName(any(), "Auth") } throws dataAccessException

        val exc = assertThrows<DataAccessException> {
            initializeAuthApp()
        }

        assertEquals(exc.message, dataAccessException.message)

        verify(exactly = 1) { appRepository.retrieveAppByName(any(), "Auth") }
        verify(exactly = 0) { userRepository.retrieveUserByUsername(any(), any(), any()) }
        verify(exactly = 0) { userRepository.retrieveUserCredentials(any(), any()) }
        verify(exactly = 0) { userRepository.upsertUserCredentials(any(), any()) }
        verify(exactly = 0) { userRepository.updateUser(any(), any()) }
    }
}
