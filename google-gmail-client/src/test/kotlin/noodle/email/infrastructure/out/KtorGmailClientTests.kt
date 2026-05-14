package noodle.email.infrastructure.out

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import noodle.email.infrastructure.serialization.GmailHistory
import noodle.email.infrastructure.serialization.GmailMessage
import noodle.email.infrastructure.serialization.GmailProfile
import noodle.email.infrastructure.serialization.GmailWatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledInNativeImage
import java.util.UUID
import kotlin.io.encoding.Base64.Default.UrlSafe

@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorGmailClientTests {
    val gmail = "damien.hoyk@gmail.com"
    val historyId = (1000L..9999L).random().toString()
    val newHistoryId = historyId + (1L..99L).random()
    val messageId = (1000..9999).random().toString()
    val emailText = "👋 Hello, World."
    val encodedMessage = UrlSafe.encode(emailText.toByteArray())
    val senderName = "Sender"
    val senderEmail = "test-${UUID.randomUUID()}@gmail.com"
    val expiration = (1000L..9999L).random()

    val engine =
        MockEngine {
            val text =
                when (it.method to it.url.encodedPath) {
                    Get to "/gmail/v1/users/me/profile" ->
                        """
                        {
                          "emailAddress": "$gmail",
                          "messagesTotal": 0,
                          "threadsTotal": 0,
                          "historyId": "$historyId"
                        }
                        """.trimIndent()

                    Get to "/gmail/v1/users/me/history" ->
                        """
                        {
                          "history": [
                            {
                              "id": "$historyId",
                              "messages": [
                                {
                                  "id": "$messageId",
                                  "threadId": "$messageId"
                                }
                              ],
                              "messagesAdded": [
                                {
                                  "message": {
                                    "id": "$messageId",
                                    "threadId": "$messageId",
                                    "labelIds": [
                                      "UNREAD",
                                      "IMPORTANT",
                                      "CATEGORY_UPDATES",
                                      "INBOX"
                                    ]
                                  }
                                }
                              ]
                            }
                          ],
                          "historyId": "$newHistoryId"
                        }
                        """.trimIndent()

                    Get to "/gmail/v1/users/me/messages/$messageId" ->
                        """
                        {
                          "id": "$messageId",
                          "threadId": "$messageId",
                          "labelIds": [
                            "UNREAD",
                            "IMPORTANT",
                            "CATEGORY_UPDATES",
                            "INBOX"
                          ],
                          "snippet": "text",
                          "payload": {
                            "partId": "",
                            "mimeType": "multipart/alternative",
                            "filename": "",
                            "headers": [
                              {
                                "name": "From",
                                "value": "$senderName \u003c$senderEmail\u003e"
                              }
                            ],
                            "body": {
                              "size": 0
                            },
                            "parts": [
                              {
                                "partId": "0",
                                "mimeType": "text/plain",
                                "body": {
                                  "size": ${encodedMessage.length},
                                  "data": "$encodedMessage"
                                }
                              }
                            ]
                          },
                          "sizeEstimate": 77048,
                          "historyId": "$historyId",
                          "internalDate": "1772296224000"
                        }
                        """.trimIndent()

                    Get to "/gmail/v1/users/me/labels" ->
                        """
                        """.trimIndent()

                    Post to "/gmail/v1/users/me/watch" ->
                        """
                        {
                          "historyId": "$historyId",
                          "expiration": $expiration
                        }
                        """.trimIndent()

                    else -> "{}"
                }

            when (it.method to it.url.encodedPath) {
                Get to "/gmail/v1/users/me/profile",
                Get to "/gmail/v1/users/me/history",
                Get to "/gmail/v1/users/me/messages/$messageId",
                Get to "/gmail/v1/users/me/labels",
                Post to "/gmail/v1/users/me/watch",
                ->
                    respond(
                        content = ByteReadChannel(text),
                        status = OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                Post to "/gmail/v1/users/me/stop" -> respondOk()
                else -> respondError(NotFound)
            }
        }

    val httpClient =
        HttpClient(engine) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

    val googleGmailClient = KtorGmailClient(httpClient)

    @Test
    fun getProfile() =
        runBlocking {
            val profile = googleGmailClient.getProfile {}.body<GmailProfile>()
            assertEquals(gmail, profile.emailAddress)
            assertEquals(historyId, profile.historyId.toString())
        }

    @Test
    fun getHistory() =
        runBlocking {
            val history = googleGmailClient.getHistory().body<GmailHistory>()
            val message = history.history.first().messagesAdded.first()
            assertEquals(newHistoryId, history.historyId.toString())
            assertEquals(messageId, message.message.id)
        }

    @Test
    fun getMessage(): Unit =
        runBlocking {
            val message =
                googleGmailClient.getMessage(messageId = messageId).body<GmailMessage>()
            assertEquals(senderEmail, message.from?.address)
            assertEquals(emailText, message.text)
        }

    @Test
    fun postStop(): Unit =
        runBlocking {
            googleGmailClient.postStop()
        }

    @Test
    fun postWatch(): Unit =
        runBlocking {
            val result = googleGmailClient.postWatch().body<GmailWatch>()
            assertEquals(historyId, result.historyId.toString())
            assertEquals(expiration, result.expiration)
        }
}
