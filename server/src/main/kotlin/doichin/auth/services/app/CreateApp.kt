package doichin.auth.services.app

import doichin.auth.AppState
import doichin.auth.dto.App
import doichin.auth.dto.AuthResource
import doichin.auth.dto.CreateAppRequest
import doichin.auth.dto.CreateRoleRequest
import doichin.auth.dto.Role
import doichin.auth.lib.ValidationException
import doichin.auth.repositories.AppRepository
import doichin.auth.repositories.PermissionRepository
import doichin.auth.repositories.ResourceRepository
import doichin.auth.repositories.RoleRepository
import doichin.auth.repositories.UserRepository
import doichin.auth.repositories.db.Database
import doichin.auth.services.role.CreateRole
import doichin.auth.services.user.CreateUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory

val createApp by lazy { CreateApp() }

class CreateApp(
    private val dslContext: DSLContext = Database.dslContext,
    private val authApp: App = AppState.authApp,
    private val appRepository: AppRepository = AppRepository(),
    private val permissionRepository: PermissionRepository = PermissionRepository(),
    private val roleRepository: RoleRepository = RoleRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val resourceRepository: ResourceRepository = ResourceRepository(),
    private val createUser: CreateUser = CreateUser(),
    private val createRole: CreateRole = CreateRole(),
) {

    companion object {
        private val log = LoggerFactory.getLogger(CreateApp::class.java)
    }

    suspend operator fun invoke(req: CreateAppRequest): App {
        validate(req)

        return withContext(Dispatchers.IO) {
            try {
               val (app, adminUser, verificationToken) = dslContext.transactionResult { configuration ->
                    val ctx = DSL.using(configuration)

                    // Retrieve information we need
                    appRepository.retrieveAppByName(ctx, req.appName)
                        ?.let { throw ValidationException("An app with this name already exists.") }

                    val app = appRepository.insertApp(ctx, req.id, req.appName)
                    val (adminUser, token) = createUser.createUserRecords(ctx, app.id, req.adminUser)
                    val adminRole = createAppAdminRole(ctx, app)
                    userRepository.upsertUserRole(ctx, adminUser, adminRole)

                    Triple(app, adminUser, token)
                }

                createUser.sendUserVerificationEmail(adminUser, verificationToken)
                log.info("Created App {}:{}", app.id, app.appName)

                app
            } catch (e: Exception) {
                // Unwrap the original exception if there is one and throw it
                if (e.cause != null) throw e.cause!! else throw e
            }
        }
    }

    private fun createAppAdminRole(ctx: DSLContext, app: App): Role {
        // Create an admin role for the new app
        val adminRole = createRole.createUserRole(
            ctx, app.id, CreateRoleRequest("Admin", "app administrator")
        )

        // Grant role management to the admin role
        resourceRepository.getByName(ctx, AuthResource.ROLE.cid())?.let { resource ->
            resourceRepository.allocate(ctx, app, resource)
            val permissions = permissionRepository.listByResourceId(ctx, resource.id)
            roleRepository.grantPermissions(ctx, adminRole, permissions)
        }

        // Grant user management to admin role
        resourceRepository.getByName(ctx, AuthResource.USER.cid())?.let { resource ->
            resourceRepository.allocate(ctx, app, resource)
            val permissions = permissionRepository.listByResourceId(ctx, resource.id)
            println(resource.id)
            roleRepository.grantPermissions(ctx, adminRole, permissions)
        }

        // Grant permission to see all allocated resource for this app
        resourceRepository.getByName(ctx, AuthResource.RESOURCE.cid())?.let { resource ->
            resourceRepository.allocate(ctx, app, resource)
            val permissions = permissionRepository.listByResourceId(ctx, resource.id)
            permissions.firstOrNull { it.action == "read_allocated" }?.let {
                roleRepository.grantPermissions(ctx, adminRole, listOf(it))
            }
        }

        return adminRole
    }

    private fun validate(req: CreateAppRequest) {
        if (req.appName.length > 20) {
            throw ValidationException("App name cannot be longer than 20 characters")
        }
    }
}
