package doichin.auth

import doichin.auth.client.ApiResponse
import doichin.auth.client.AuthClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoginApiTest: BaseApiTest() {

    @BeforeEach
    fun setUp() {
        authClient = AuthClient(Url(""), client)
    }

    @Test
    @DisplayName("success - user login")
    fun testUserLogin() = runTest {
        val resp = authClient.login("root", dotenv["ROOT_PASSWORD"])
        assertNotNull(resp)
    }

    @Test
    @DisplayName("failure - bad login")
    fun testUserLoginFailure() = runTest {
        val resp = authClient.login("no user", "bad password")
        assertTrue(resp is ApiResponse.Failure)
        assertEquals(resp.statusCode, HttpStatusCode.Forbidden.value)
    }
}