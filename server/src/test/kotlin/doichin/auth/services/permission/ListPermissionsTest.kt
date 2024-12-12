package doichin.auth.services.permission

import doichin.auth.BaseTest
import doichin.auth.dto.Listing
import doichin.auth.dto.Permission
import doichin.auth.dto.generate
import doichin.auth.repositories.PermissionRepository
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


@MockKExtension.CheckUnnecessaryStub
class ListPermissionsTest: BaseTest() {
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var listPermissions: ListPermissions

    @BeforeEach
    fun setUp() {
        permissionRepository = mockk<PermissionRepository>()
        listPermissions = ListPermissions(dslContext, permissionRepository)
    }

    @Test
    fun `success - list permissions for auth app`() = runTest {
        val permission1 = Permission.generate()
        val permission2 = Permission.generate()
        val items = listOf(permission1, permission2)

        val offset = 0L
        val limit = 10L
        val expected = Listing<Permission>(items, offset, limit, items.count().toLong())

        every { permissionRepository.list(dslContext, offset, limit) } returns listOf(permission1, permission2)
        every { permissionRepository.count(dslContext) } returns items.count().toLong()

        val actual = listPermissions(offset, limit)

        assertEquals(expected, actual)
    }
}