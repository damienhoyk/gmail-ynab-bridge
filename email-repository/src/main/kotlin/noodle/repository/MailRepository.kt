package noodle.repository

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import java.time.Instant

class MailRepository(
    private val client: DynamoDbClient = DynamoDbClient.create()
) {

    val table = "mail"
    val partitionKey = "address"
    val sortKey = "mailId"

    suspend fun put(item: Map<String, AttributeValue>) = withContext(IO) {
        val item = item.toMutableMap()
        val time = Instant.now().epochSecond
        item["modified"] = fromN(time.toString())
        client.putItem { it.tableName(table).item(item) }
    }

    suspend fun get(address: String) = withContext(IO) {
        val key = mapOf(partitionKey to fromS(address))
        client.getItem { it.tableName(table).key(key) }
    }

}