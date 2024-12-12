package doichin.auth

import doichin.auth.client.*
import doichin.auth.dto.AllocateResourceRequest
import doichin.auth.dto.App
import doichin.auth.dto.CreateAppRequest
import doichin.auth.dto.CreateResourceRequest
import doichin.auth.dto.Resource
import doichin.auth.dto.generate
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ResourceApiTest: BaseApiTest() {

    private val createAppReq = CreateAppRequest.generate()
    private val createResourceReq = CreateResourceRequest.generate()
    private val allocateReq = AllocateResourceRequest(createResourceReq.id, createAppReq.id)

    @BeforeEach
    fun setUp() {
        authClient = AuthClient(Url(""), client)
    }

    @Test
    @DisplayName("failure - not authenticated - create resource")
    fun notAuthenticatedCreate() = runTest {
        val resp = authClient.createResource(createResourceReq)
        assertTrue(resp is ApiResponse.Failure)
    }

    @Test
    @DisplayName("failure - not authenticated - allocate resource")
    fun notAuthenticatedAllocate() = runTest {
        val resp = authClient.allocateResource(allocateReq)
        assertTrue(resp is ApiResponse.Failure)
    }

    @Test
    @DisplayName("failure - not authenticated - get resource")
    fun notAuthenticatedGet() = runTest {
        val resp = authClient.getResource(Uuid.random())
        assertTrue(resp is ApiResponse.Failure)
    }

    @Test
    @DisplayName("failure - not authenticated - list resource")
    fun notAuthenticatedList() = runTest {
        val resp = authClient.listResources()
        assertTrue(resp is ApiResponse.Failure)
    }

    @Test
    @DisplayName("failure - get non existent resource")
    fun getNotExistentResource() = runTest {
        authClient.login("root", dotenv["ROOT_PASSWORD"])

        val resp = authClient.getResource(Uuid.random())
        assertTrue(resp is ApiResponse.Failure)
    }

    @Test
    @DisplayName("success - create app and allocate a resource")
    fun createAndAllocate() = runTest {
        authClient.login("root", dotenv["ROOT_PASSWORD"])
        val appResp = authClient.createApp(createAppReq)
        assertIs<ApiResponse.Success<App>>(appResp)

        val listResp = authClient.listResources()
        assertTrue(listResp is ApiResponse.Success)

        val resResp = authClient.createResource(createResourceReq)
        assertIs<ApiResponse.Success<Resource>>(resResp)

        val allocateResp = authClient.allocateResource(allocateReq)
        assertIs<ApiResponse.Success<List<Resource>>>(allocateResp)

        val getResp = authClient.getResource(resResp.data.id)
        assertIs<ApiResponse.Success<Resource>>(getResp)

        val listResp2 = authClient.listResources()
        assertTrue(listResp2 is ApiResponse.Success)
        assertEquals(listResp2.data.total, listResp.data.total + 1)
    }

}