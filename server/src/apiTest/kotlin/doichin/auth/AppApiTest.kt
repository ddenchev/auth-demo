package doichin.auth

import doichin.auth.client.*
import doichin.auth.dto.App
import doichin.auth.dto.CreateAppRequest
import doichin.auth.dto.CreateUserRequest
import doichin.auth.dto.Listing
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class AppApiTest: BaseApiTest() {

    private val testUser = CreateUserRequest("test${Random.nextInt()}", "test@test${Random.nextInt()}.com")

    @BeforeEach
    fun setUp() {
        authClient = AuthClient(Url(""), client)
    }

    @Test
    @DisplayName("failure - not authenticated")
    fun notAuthenticated() = runTest {
        val resp = authClient.listApps()
        assertTrue(resp is ApiResponse.Failure)
    }

    @Test
    @DisplayName("success - get auth app")
    fun getApp() = runTest {
        authClient.login("root", dotenv["ROOT_PASSWORD"])
        val resp = authClient.getApp(Uuid.fromLongs(0,0))
        assertTrue(resp is ApiResponse.Success<App>)
        assertEquals(resp.data.appName, "Auth")
    }

    @Test
    @DisplayName("failure - app not found")
    fun getAnUnknownApp() = runTest {
        authClient.login("root", dotenv["ROOT_PASSWORD"])
        val resp = authClient.getApp(Uuid.random())
        assertTrue(resp is ApiResponse.Failure)
        assertEquals(resp.statusCode, HttpStatusCode.NotFound.value)
    }


    @Test
    @DisplayName("success - create and list apps")
    fun appTest() = runTest {
        authClient.login("root", dotenv["ROOT_PASSWORD"])
        val resp = authClient.listApps()
        assertIs<ApiResponse.Success<Listing<App>>>(resp)

        val appName = Uuid.random().toString().substring(0, 10)
        val createAppRequest = CreateAppRequest(appName, testUser)
        val newApp = authClient.createApp(createAppRequest)
        assertTrue(newApp is ApiResponse.Success)

        val resp2 = authClient.listApps()
        assertTrue(resp2 is ApiResponse.Success)
        assertEquals(resp.data.total +1, resp2.data.total)

        // TODO: Add assertions about the admin user, roles and permissions
    }



}