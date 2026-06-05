package noodle.google.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType.Application
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.contentType
import noodle.ktor.defaultLogging

public class GoogleOAuth2Api(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val urlString = "https://oauth2.googleapis.com/"
    private val httpClient =
        httpClient.config {
            defaultLogging()
            defaultRequest {
                contentType(Application.Json)
                url(urlString)
            }

            block()
        }

    public suspend fun requestTokenInfo(parameters: ParametersBuilder.() -> Unit): HttpResponse =
        httpClient
            .submitForm("tokeninfo", Parameters.build { parameters() })
}
