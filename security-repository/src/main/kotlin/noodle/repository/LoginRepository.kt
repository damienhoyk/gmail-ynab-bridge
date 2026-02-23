package noodle.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class LoginRepository(
    private val table: String = "login",
    private val client: DynamoDbClient = DynamoDbClient.create()
) {

    fun get(id: String?) = client.getItem {
        val key = mapOf("id" to fromS(id))
        it.tableName(table).key(key)
    }

    suspend fun put(item: Map<String, AttributeValue>) = withContext(Dispatchers.IO) {
        client.putItem { it.tableName(table).item(item) }
    }

}