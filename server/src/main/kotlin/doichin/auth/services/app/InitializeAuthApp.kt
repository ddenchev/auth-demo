package doichin.auth.services.app

import doichin.auth.dto.AllocateResourceRequest
import doichin.auth.dto.App
import doichin.auth.dto.CreatePermissionRequest
import doichin.auth.dto.CreateResourceRequest
import doichin.auth.dto.CreateRoleRequest
import doichin.auth.dto.CreateUserRequest
import doichin.auth.dto.Permission
import doichin.auth.dto.Role
import doichin.auth.dto.User
import doichin.auth.dto.UserCredentials
import doichin.auth.dto.UserStatus
import doichin.auth.lib.generatePasswordHash
import doichin.auth.lib.generateRandom
import doichin.auth.lib.verifyPassword
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.Database
import doichin.auth.services.resource.AllocateResource
import doichin.auth.services.resource.CreateResource
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import kotlin.uuid.Uuid
import doichin.auth.services.resource.allocateResource as allocateResourceService
import doichin.auth.services.resource.createResource as createResourceService

val initializeAuthApp by lazy { InitializeAuthApp() }

class InitializeAuthApp(
    private val dslContext: DSLContext = Database.dslContext,
    private val appRepository: AppRepository = AppRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val roleRepository: RoleRepository = RoleRepository(),
    private val permissionRepository: PermissionRepository = PermissionRepository(),
    private val createResource: CreateResource = createResourceService,
    private val allocateResource: AllocateResource = allocateResourceService,
) {
    private val env = dotenv{
        directory = ".."
        filename = ".env"
    }

    suspend operator fun invoke(): App {
        return withContext(Dispatchers.IO) {
            try {
                dslContext.transactionResult { configuration ->
                    val ctx = DSL.using(configuration)

                    // If auth app has not been initialized yet (first run), initialize it
                    val app = appRepository.retrieveAppByName(ctx, "Auth")
                        ?: initializeAuthApp(ctx)

                    val rootUser = userRepository
                        .retrieveUserByUsername(ctx, app.id, "root")
                        ?: throw IllegalStateException("The root user doesn't exist.")

                    // Verify the password hasn't changed
                    val userCredentials = userRepository
                        .retrieveUserCredentials(ctx, rootUser.id)
                        ?: throw IllegalStateException("The root user doesn't have credentials")

                    // If it has changed, reset the password
                    if (!verifyPassword(env["ROOT_PASSWORD"], userCredentials)) {
                        initRootUserCredentials(ctx, rootUser)
                    }

                    app
                }
            } catch (e: Exception) {
                // Unwrap the original exception if there is one and throw it
                if (e.cause != null) throw e.cause!! else throw e
            }
        }
    }

    private fun initializeAuthApp(ctx: DSLContext): App {
        val authApp = appRepository.insertApp(ctx, Uuid.fromLongs(0, 0), "Auth")

        // Create the Auth app itself
        val createUserRequest = CreateUserRequest("root", "")

        // Create the root user
        var rootUser = userRepository.insertUser(ctx, authApp.id, createUserRequest)
        rootUser = initRootUserCredentials(ctx, rootUser)

        // Initialize auth app permissions and admin role
        val adminRole = initializeAuthAppPermissions(ctx, authApp)
        userRepository.upsertUserRole(ctx, rootUser, adminRole)

        return authApp
    }

    private fun initRootUserCredentials(ctx: DSLContext, rootUser: User): User {

        // Setup user credentials
        val salt = generateRandom()
        val passwordHash = generatePasswordHash(env["ROOT_PASSWORD"], salt)
        val userCredentials = UserCredentials(rootUser.id, passwordHash, salt)
        userRepository.upsertUserCredentials(ctx, userCredentials)

        // Set the user status to verified
        val updatedUser = rootUser.copy(userStatus = UserStatus.VERIFIED)
        return userRepository.updateUser(ctx, updatedUser)
            ?: throw IllegalStateException()
    }

    private fun initializeAuthAppPermissions(ctx: DSLContext, authApp: App): Role {
        // create admin role
        val adminRole = roleRepository.insertRole(ctx, authApp.id, CreateRoleRequest("Admin", "App administrator"))

        // Initialize the auth resources
        val permissions = initializeAuthResources(ctx, authApp)

        // Assign all permissions to the admin role
        roleRepository.insertRolePermissions(ctx, adminRole.id, permissions)

        return adminRole
    }

    private fun initializeAuthResources(ctx: DSLContext, authApp: App): MutableList<Permission> {
        val createResource = createResource.withContext(ctx)
        val allocateResource = allocateResource.withContext(ctx)
        val permissions: MutableList<Permission> = mutableListOf()

        // --------------------------------------------
        // Create App Resource and App Permissions
        // --------------------------------------------
        val appResource = createResource(
            CreateResourceRequest(
                "app",
                "A user facing application that leverages one or more resources",
            )
        )
        allocateResource(AllocateResourceRequest(appResource.id, authApp.id))
        permissions.addAll(listOf(
            CreatePermissionRequest(appResource.id, "create", "Create a new app"),
            CreatePermissionRequest(appResource.id, "read", "Read apps"),
            CreatePermissionRequest(appResource.id, "delete", "Delete an existing apps"),
        ).map { req ->
            permissionRepository.insertPermission(ctx, req)
        })


        // --------------------------------------------
        // Create Resource Resource and Resource Permissions
        // --------------------------------------------
        val resourceResource = createResource(
            CreateResourceRequest(
                "resource",
                "A resource that is being access controlled by this service",
            )
        )
        allocateResource(AllocateResourceRequest(resourceResource.id, authApp.id))
        permissions.addAll(listOf(
            CreatePermissionRequest(resourceResource.id, "create", "Create new resources"),
            CreatePermissionRequest(resourceResource.id, "read", "Read the master set of resources"),
            CreatePermissionRequest(resourceResource.id, "allocate", "Allocate a resource to an app"),
            CreatePermissionRequest(resourceResource.id, "read_allocated", "Read allocated resources for an app"),
        ).map { req ->
            permissionRepository.insertPermission(ctx, req)
        })


        // --------------------------------------------
        // Create Permission Resource and Permission Permissions
        // --------------------------------------------
        val permissionResource = createResource(
            CreateResourceRequest(
                "permission",
                "Represents a permission to access a resource",
            )
        )
        allocateResource(AllocateResourceRequest(permissionResource.id, authApp.id))
        permissions.addAll(listOf(
            CreatePermissionRequest(permissionResource.id, "create", "Create a new permission"),
            CreatePermissionRequest(permissionResource.id, "read", "Read permissions"),
            CreatePermissionRequest(permissionResource.id, "delete", "Delete an existing permission"),
        ).map { req ->
            permissionRepository.insertPermission(ctx, req)
        })


        // --------------------------------------------
        // Create Role Resource and Role Permissions
        // --------------------------------------------
        val roleResource = createResource(
            CreateResourceRequest(
                "role",
                "A group of permissions that is assignable to a user",
            )
        )
        allocateResource(AllocateResourceRequest(roleResource.id, authApp.id))
        permissions.addAll(listOf(
            CreatePermissionRequest(roleResource.id, "create", "Create a new role"),
            CreatePermissionRequest(roleResource.id, "read", "Read roles"),
            CreatePermissionRequest(roleResource.id, "manage_permissions", "Add or delete permission to an existing role"),
            CreatePermissionRequest(roleResource.id, "delete", "Delete an existing role"),
        ).map { req ->
            permissionRepository.insertPermission(ctx, req)
        })

        // --------------------------------------------
        // Create User Resource and User Permissions
        // --------------------------------------------
        val userResource = createResource(
            CreateResourceRequest(
                "user",
                "A person who may be authorized to use a subset of resources",
            )
        )
        allocateResource(AllocateResourceRequest(userResource.id, authApp.id))
        permissions.addAll(listOf(
            CreatePermissionRequest(userResource.id, "create", "Create a new user"),
            CreatePermissionRequest(userResource.id, "read", "Read users"),
            CreatePermissionRequest(userResource.id, "manage_roles", "Manage user roles"),
            CreatePermissionRequest(userResource.id, "delete", "Delete an existing role")

        ).map { req ->
            permissionRepository.insertPermission(ctx, req)
        })

        return permissions
    }
}