package noodle.repository

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class BridgeRepository(val table: String = "bridge", val client: DynamoDbClient = DynamoDbClient.create()) {

    val partitionKey = "source"

    suspend fun get(address: String) = withContext(IO) {
        client.query {
            it.tableName(table)
                .expressionAttributeNames(mapOf("#s" to "source"))
                .expressionAttributeValues(mapOf(":s" to fromS(address)))
                .keyConditionExpression("#s = :s")
        }
    }

    suspend fun updateHistoryId(source: String, destination: String, historyId: String) = withContext(IO) {
        client.updateItem {
            val key = mapOf("source" to fromS(source), "destination" to fromS(destination))
            val names = mapOf("#h" to "historyId")
            val values = mapOf(":h" to fromS(historyId))

            it.tableName(table).key(key)
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .updateExpression("set #h = :h")
        }
    }

    suspend fun queryAttribute(source: String, attribute: String) = withContext(IO) {
        client.query {
            it.tableName(table).keyConditionExpression("#s = :s")
                .expressionAttributeNames(mapOf("#s" to "source", "#d" to attribute))
                .expressionAttributeValues(mapOf(":s" to fromS(source)))
                .projectionExpression("#d")
        }.items().mapNotNull { it[attribute]?.s() }
    }

    suspend fun updateStatus(source: String, destination: String, status: String) = withContext(IO) {
        client.updateItem {
            val key = mapOf("source" to fromS(source), "destination" to fromS(destination))
            val names = mapOf("#s" to "status")
            val values = mapOf(":s" to fromS(status))

            it.tableName(table).key(key)
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .updateExpression("set #s = :s")
        }
    }

    suspend fun updateStatusMutex(source: String, destination: String, status: String) = withContext(IO) {
        client.updateItem {
            val key = mapOf("source" to fromS(source), "destination" to fromS(destination))
            val names = mapOf("#s" to "status")
            val values = mapOf(":s" to fromS(status))

            it.tableName(table).key(key)
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .conditionExpression("attribute_not_exists(#s) or not #s = :s")
                .updateExpression("set #s = :s")
                .returnValues("ALL_NEW")
        }
    }

}