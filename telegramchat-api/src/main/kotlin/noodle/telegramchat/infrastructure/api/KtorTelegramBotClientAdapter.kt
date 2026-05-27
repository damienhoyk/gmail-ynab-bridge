package noodle.telegramchat.infrastructure.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import noodle.telegram.infrastructure.api.KtorTelegramBotClient
import noodle.telegramchat.core.port.TelegramBotClient

class KtorTelegramBotClientAdapter(
    httpClient: HttpClient,
    block: HttpClientConfig<*>.() -> Unit = {},
) : KtorTelegramBotClient(httpClient, block),
    TelegramBotClient
