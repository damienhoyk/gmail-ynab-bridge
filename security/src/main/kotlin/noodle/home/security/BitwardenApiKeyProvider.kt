package noodle.home.security

import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings

class BitwardenApiKeyProvider(
    val secretName: String,
    val client: BitwardenClient = BitwardenClient(BitwardenSettings()),
    val organizationId: String
) : AccessTokenProvider {

    override suspend fun getToken(id: String?) = getNewToken()
    override suspend fun getNewToken(id: String?) = client.secrets().getApiKey(organizationId, secretName)!!

}