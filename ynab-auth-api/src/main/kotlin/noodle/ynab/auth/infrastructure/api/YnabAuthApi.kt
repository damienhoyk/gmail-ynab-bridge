package noodle.ynab.auth.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import noodle.ktor.defaultJson
import noodle.ktor.defaultLogging

public class YnabAuthApi(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) {
    private val httpClient: HttpClient =
        httpClient.config {
            defaultLogging()
            defaultJson()
            block()
        }

    public suspend fun postToken(form: Parameters): HttpResponse = httpClient.post("https://app.ynab.com/oauth/token") { setBody(FormDataContent(form)) }
}
