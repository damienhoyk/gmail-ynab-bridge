package noodle.tokenrefresher.bootstrap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import noodle.bitwarden.infrastructure.api.Bitwarden
import noodle.bitwarden.infrastructure.api.bitwardenSecret
import noodle.oauth2.infrastructure.api.OidcApi
import noodle.tokenrefresher.core.port.OAuth2TokenProvider
import noodle.tokenrefresher.core.service.RefreshTokensService
import noodle.tokenrefresher.infrastructure.api.google.KtorGoogleTokenRefresher
import noodle.tokenrefresher.infrastructure.api.ynab.KtorYnabTokenRefresher
import noodle.tokenrefresher.infrastructure.persistence.DynamoDbTokenRepository
import noodle.ynab.auth.infrastructure.api.YnabAuthApi
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.io.InputStream
import java.io.OutputStream

public class TokenRefreshHandler : RequestStreamHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    private val initScope = CoroutineScope(Default)

    private val credentialsProviderAsync = initScope.async { DefaultCredentialsProvider.builder().build() }
    private val urlConnectionClient = UrlConnectionHttpClient.builder()

    private val dynamoDbClientAsync =
        initScope.async {
            DynamoDbClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val secretsManagerClientAsync =
        initScope.async {
            SecretsManagerClient
                .builder()
                .credentialsProvider(credentialsProviderAsync.await())
                .httpClientBuilder(urlConnectionClient)
                .build()
        }

    private val engineAsync = initScope.async { Java.create() }
    private val bitwardenAsync = initScope.async { Bitwarden(secretsManagerClientAsync.await()) }

    private val googleSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("google")?.bitwardenSecret() ?: error("google secret missing") }
    private val ynabSecretAsync =
        initScope.async { bitwardenAsync.await().getSecret("ynab")?.bitwardenSecret() ?: error("ynab secret missing") }

    private val googleTokenRefresherAsync =
        initScope.async {
            val httpClient = HttpClient(engineAsync.await())
            val oidcApi = OidcApi("https://accounts.google.com/.well-known/openid-configuration", httpClient)
            val secret = googleSecretAsync.await()
            KtorGoogleTokenRefresher(oidcApi, secret.clientId ?: error("google clientId secret missing"), secret.clientSecret ?: error("google clientSecret secret missing"))
        }

    private val ynabTokenRefresherAsync =
        initScope.async {
            val httpClient = HttpClient(engineAsync.await())
            val ynabAuthApi = YnabAuthApi(httpClient)
            val secret = ynabSecretAsync.await()
            KtorYnabTokenRefresher(ynabAuthApi, secret.clientId ?: error("ynab clientId secret missing"), secret.clientSecret ?: error("ynab clientSecret secret missing"))
        }

    private val providersAsync =
        initScope.async {
            mapOf(
                "google" to (googleTokenRefresherAsync.await() as OAuth2TokenProvider),
                "ynab" to (ynabTokenRefresherAsync.await() as OAuth2TokenProvider),
            )
        }

    private val tokenRepositoryAsync =
        initScope.async { DynamoDbTokenRepository(dynamoDbClientAsync.await()) }

    private val service =
        initScope.async {
            RefreshTokensService(
                tokens = tokenRepositoryAsync.await(),
                providers = providersAsync.await(),
            )
        }

    public override fun handleRequest(
        input: InputStream,
        output: OutputStream,
        context: Context,
    ) {
        runBlocking {
            try {
                service.await().execute()
                log.info("Token refresh completed successfully")
            } catch (e: Exception) {
                log.error("Token refresh failed", e)
                throw e
            }
        }
        output.write("\"ok\"".toByteArray())
    }
}
