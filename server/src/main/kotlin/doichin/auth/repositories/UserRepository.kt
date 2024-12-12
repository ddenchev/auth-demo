package doichin.auth.repositories

import doichin.auth.dto.CreateUserRequest
import doichin.auth.dto.Role
import doichin.auth.dto.User
import doichin.auth.dto.UserCredentials
import doichin.auth.services.user.PasswordResetRequest
import kotlinx.datetime.Instant
import org.jooq.DSLContext
import org.jooq.generated.Tables.APP_USER
import org.jooq.generated.Tables.APP_USER_CREDENTIALS
import org.jooq.generated.Tables.APP_USER_PASSWORD_RESET_REQUEST
import org.jooq.generated.Tables.APP_USER_ROLE
import org.jooq.generated.Tables.ROLE
import org.jooq.generated.enums.UserStatus
import org.jooq.generated.tables.records.AppUserCredentialsRecord
import org.jooq.generated.tables.records.AppUserPasswordResetRequestRecord
import org.jooq.generated.tables.records.AppUserRecord
import org.jooq.generated.tables.records.RoleRecord
import kotlin.uuid.Uuid
import doichin.auth.dto.UserStatus as UserStatusDto

class UserRepository {

    fun insertUser(
        ctx: DSLContext,
        appId: Uuid,
        createUserRequest: CreateUserRequest,
    ): User {
        return ctx
            .insertInto(APP_USER)
            .set(APP_USER.APP_ID, appId)
            .set(APP_USER.USERNAME, createUserRequest.username)
            .set(APP_USER.EMAIL, createUserRequest.email)
            .set(APP_USER.FIRST_NAME, createUserRequest.firstName)
            .set(APP_USER.LAST_NAME, createUserRequest.lastName)
            .set(APP_USER.USER_STATUS, UserStatus.PENDING)
            .returning()
            .fetchOne()
            ?.toDto()
            ?: throw IllegalStateException()
    }

    fun updateUser(
        ctx: DSLContext,
        user: User,
    ): User? {
        return ctx
            .update(APP_USER)
            .set(user.toRecord())
            .where(APP_USER.ID.eq(user.id))
            .returning()
            .fetchOne()
            ?.toDto()
    }

    fun insertPasswordResetToken(
        ctx: DSLContext,
        userId: Uuid,
        passwordResetToken: String,
        expiry: Instant
    ): String {
        return ctx
            .insertInto(APP_USER_PASSWORD_RESET_REQUEST)
            .set(APP_USER_PASSWORD_RESET_REQUEST.USER_ID, userId)
            .set(APP_USER_PASSWORD_RESET_REQUEST.PASSWORD_RESET_TOKEN, passwordResetToken)
            .set(APP_USER_PASSWORD_RESET_REQUEST.EXPIRY,expiry)
            .returning()
            .fetchOne()
            ?.passwordResetToken
            ?: throw IllegalStateException()
    }

    fun getPasswordResetToken(ctx: DSLContext, passwordResetToken: String): PasswordResetRequest? {
        return ctx.select()
            .from(APP_USER_PASSWORD_RESET_REQUEST)
            .where(APP_USER_PASSWORD_RESET_REQUEST.PASSWORD_RESET_TOKEN.eq(passwordResetToken))
            .fetchOneInto(AppUserPasswordResetRequestRecord::class.java)
            ?.toDto()
    }

    fun upsertUserCredentials(
        ctx: DSLContext,
        userCredentials: UserCredentials
    ): UserCredentials? {
        return ctx
            .insertInto(APP_USER_CREDENTIALS)
            .set(APP_USER_CREDENTIALS.USER_ID, userCredentials.userId)
            .set(APP_USER_CREDENTIALS.PASSWORD_HASH, userCredentials.passwordHash)
            .set(APP_USER_CREDENTIALS.PASSWORD_SALT, userCredentials.passwordSalt)
            .onDuplicateKeyUpdate()
            .set(APP_USER_CREDENTIALS.PASSWORD_HASH, userCredentials.passwordHash)
            .set(APP_USER_CREDENTIALS.PASSWORD_SALT, userCredentials.passwordSalt)
            .returning()
            .fetchOne()
            ?.toDto()
    }

    fun upsertUserRole(
        ctx: DSLContext,
        user: User,
        role: Role
    ): List<Role> {
        ctx
            .insertInto(APP_USER_ROLE)
            .set(APP_USER_ROLE.USER_ID, user.id)
            .set(APP_USER_ROLE.ROLE_ID, role.id)
            .onDuplicateKeyUpdate()
            .set(APP_USER_ROLE.USER_ID, user.id)
            .set(APP_USER_ROLE.ROLE_ID, role.id)
            .execute()

        return ctx.select()
            .from(
                APP_USER_ROLE
                .innerJoin(ROLE).on(APP_USER_ROLE.ROLE_ID.eq(ROLE.ID))
            )
            .where(
                APP_USER_ROLE.USER_ID.eq(user.id)
            )
            .fetchInto(RoleRecord::class.java)
            .map { it.toDto() }
    }

    fun retrieveUserCredentials(
        ctx: DSLContext,
        userId: Uuid
    ): UserCredentials? {
        return ctx
            .select()
            .from(APP_USER_CREDENTIALS)
            .where(APP_USER_CREDENTIALS.USER_ID.eq(userId))
            .fetchOneInto(AppUserCredentialsRecord::class.java)
            ?.toDto()
    }

    fun getByIdAppId(ctx: DSLContext, appId: Uuid, userId: Uuid): User? {
        return ctx.select()
            .from(APP_USER)
            .where(
                APP_USER.ID.eq(userId)
                .and(APP_USER.APP_ID.eq(appId)))
            .fetchOneInto(AppUserRecord::class.java)
            ?.toDto()
    }

    fun getById(ctx: DSLContext, userId: Uuid): User? {
        return ctx.select()
            .from(APP_USER)
            .where(APP_USER.ID.eq(userId))
            .fetchOneInto(AppUserRecord::class.java)
            ?.toDto()
    }

    fun retrieveUserByUsername(ctx: DSLContext, appId: Uuid, username: String): User? {
        return ctx.select()
            .from(APP_USER)
            .where(APP_USER.USERNAME.eq(username).and(APP_USER.APP_ID.eq(appId)))
            .fetchOneInto(AppUserRecord::class.java)
            ?.toDto()
    }

    fun listByAppId(ctx: DSLContext, appId: Uuid, offset: Long = 0, limit: Long = 20): List<User> {
        return ctx.select()
            .from(APP_USER)
            .where(APP_USER.APP_ID.eq(appId))
            .offset(offset)
            .limit(limit)
            .fetchInto(AppUserRecord::class.java)
            .map { it.toDto() }
    }

    fun countByAppId(ctx: DSLContext, appId: Uuid): Long {
        return ctx
            .selectCount()
            .from(APP_USER)
            .where(APP_USER.APP_ID.eq(appId))
            .fetchOneInto(Long::class.java)
            ?: 0L
    }

    private fun AppUserRecord.toDto(): User {
        return User(
            id = this.id,
            appId = this.appId,
            username = this.username,
            email = this.email,
            firstName = this.firstName,
            lastName = this.lastName,
            userStatus = this.userStatus.toDto()
        )
    }

    private fun User.toRecord(): AppUserRecord {
        return AppUserRecord(
            this.id,
            this.appId,
            this.username,
            this.firstName,
            this.lastName,
            this.email,
            this.userStatus.toRecord()
        )
    }

    private fun AppUserCredentialsRecord.toDto(): UserCredentials {
        return UserCredentials(
            this.userId,
            this.passwordHash,
            this.passwordSalt
        )
    }

    private fun UserStatus.toDto(): UserStatusDto =
        when (this) {
            UserStatus.PENDING -> UserStatusDto.PENDING
            UserStatus.VERIFIED -> UserStatusDto.VERIFIED
            UserStatus.DEACTIVATED -> UserStatusDto.DEACTIVATED
        }

    private fun UserStatusDto.toRecord(): UserStatus =
        when (this) {
            UserStatusDto.PENDING -> UserStatus.PENDING
            UserStatusDto.VERIFIED -> UserStatus.VERIFIED
            UserStatusDto.DEACTIVATED -> UserStatus.DEACTIVATED
        }



    private fun AppUserPasswordResetRequestRecord.toDto(): PasswordResetRequest {
        return PasswordResetRequest(
            this.userId,
            this.passwordResetToken,
            this.expiry
        )
    }
}