package doichin.auth.services.email

import aws.sdk.kotlin.services.ses.SesClient
import aws.sdk.kotlin.services.ses.model.*
import doichin.auth.AppState
import doichin.auth.EnvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SendEmail(
    private val client: SesClient = AppState.emailClient,
    private val appState: AppState = AppState,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(SendEmail::class.java)
    }

    /**
     * Sends an email using AWS SES.
     *
     * @param email The email details including recipient, sender, subject, and body.
     * @return The message ID of the email that was sent.
     * @throws Exception If there is an error sending the email.
     */
    suspend operator fun invoke(email: Email): String {
        if (appState.ENV_TYPE == EnvType.TEST) {
            log.info("Skipping sending email in test environment: $email")
            return "emailNotSend"
        }

        if (appState.ENV_TYPE == EnvType.DEV && !appState.FORCE_DEV_EMAIL) {
            log.info("Skipping sending email in development environment: $email. " +
                    "If you would like to send a specific email, please add an ENV variable FORCE_DEV_EMAIL.")
        }

        val sendEmailRequest = SendEmailRequest {
            source = email.from
            destination = Destination {
                toAddresses = email.to
            }
            message = Message {
                subject = Content {
                    data = email.subject
                }
                body = Body {
                    text = Content {
                        data = email.body
                    }
                }
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = client.sendEmail(sendEmailRequest)
                log.info("Email sent! Message ID: {}", response.messageId)
                response.messageId
            } catch (e: Exception) {
                log.error("Error sending email", e)
                throw e
            }
        }
    }
}


data class Email(
    val to: List<String>,
    val from: String,
    val subject: String,
    val body: String,
)