package noodle.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class UserRepository(private val client: DynamoDbClient = DynamoDbClient.create()) {

    private val table: String = "user"

    suspend fun get(id: String) = withContext(Dispatchers.IO) {
        client.getItem {
            val key = mapOf("id" to fromS(id))
            it.tableName(table).key(key)
        }
    }

    suspend fun put(item: Map<String, AttributeValue>) = withContext(Dispatchers.IO) {
        client.putItem { it.tableName(table).item(item) }
    }

    suspend fun query(id: String) = withContext(Dispatchers.IO) {
        client.query {
            it.tableName(table)
                .keyConditionExpression("#i = :i")
                .expressionAttributeNames(mapOf("#i" to "id"))
                .expressionAttributeValues(mapOf(":i" to fromS(id)))
        }
    }

}