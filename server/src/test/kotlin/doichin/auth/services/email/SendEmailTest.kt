package doichin.auth.services.email

import aws.sdk.kotlin.services.ses.SesClient
import aws.sdk.kotlin.services.ses.model.SendEmailResponse
import doichin.auth.BaseTest
import doichin.auth.EnvType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

@MockKExtension.CheckUnnecessaryStub
class SendEmailTest: BaseTest() {
    lateinit var sesClient: SesClient
    lateinit var sendEmail: SendEmail
    lateinit var email: Email

    @BeforeEach
    fun setUp() {
        sesClient = mockk()
        every { sesClient.close() } returns Unit

        email = Email(
            to = listOf("testUser@gmail.com"),
            from = "anotherTestUser@gmail.com",
            subject = "Email Test",
            body = "Email test",
        )

        sendEmail = SendEmail(sesClient)

        // Since we are testing an environment aware function, reset the env
        appState.ENV_TYPE = EnvType.DEV
    }

    @AfterEach
    fun tearDown() {
        appState.ENV_TYPE = EnvType.TEST
    }

    @Test
    fun `success - send an email`() = runTest {
        val expectedResponse = mockk<SendEmailResponse>()

        coEvery { sesClient.sendEmail((any())) } returns expectedResponse
        every { expectedResponse.messageId } returns "123"

        val actualMessageId = sendEmail(email)

        assertEquals(expectedResponse.messageId, actualMessageId)
    }

    @Test
    fun `failure - email fails to send`() = runTest {
        coEvery { sesClient.sendEmail((any())) } throws Exception()

        assertThrows<Exception> {
            sendEmail(email)
        }
    }


}