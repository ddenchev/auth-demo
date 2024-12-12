package doichin.auth.services.role

import doichin.auth.BaseTest
import doichin.auth.dto.Listing
import doichin.auth.dto.Role
import doichin.auth.dto.generate
import doichin.auth.repositories.RoleRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class ListRolesTest : BaseTest() {
    private lateinit var roleRepository: RoleRepository
    private lateinit var listRoles: ListRoles

    private val appId = Uuid.random()

    @BeforeEach
    fun setUp() {
        roleRepository = mockk<RoleRepository>()
        listRoles = ListRoles(dslContext, roleRepository)
    }

    @Test
    fun `success - list roles for app`() = runTest {
        val role1 = Role.generate(appId)
        val role2 = Role.generate(appId)
        val items = listOf(role1, role2)

        val offset = 0L
        val limit = 10L
        val totalCount = items.count().toLong()
        val expected = Listing<Role>(items, offset, limit, totalCount)

        every { roleRepository.listByAppId(dslContext, appId, offset, limit) } returns items
        every { roleRepository.countByAppId(dslContext, appId) } returns totalCount

        val actual = listRoles(appId, offset, limit)

        assertEquals(expected, actual)
    }
}
