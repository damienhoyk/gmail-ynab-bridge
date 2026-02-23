package noodle.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class TokenRepository(
    private val table: String = "token",
    private val client: DynamoDbClient = DynamoDbClient.create()
) {

    suspend fun put(item: Map<String, AttributeValue>) = withContext(Dispatchers.IO) {
        client.putItem { it.tableName(table).item(item) }
    }

}