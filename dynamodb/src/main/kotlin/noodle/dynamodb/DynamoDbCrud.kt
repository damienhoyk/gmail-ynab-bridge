package noodle.dynamodb

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.ReturnValue
import java.time.Instant.now

abstract class DynamoDbCrud {
    typealias Item = MutableMap<String, AttributeValue>
    typealias Key = Map<String, AttributeValue>

    protected abstract val client: DynamoDbClient

    abstract val table: String

    suspend fun put(
        key: Key,
        block: Item.() -> Unit = {},
    ) = withContext(IO) {
        val item = mutableMapOf<String, AttributeValue>().apply(block)
        item["modified"] = fromN("${now().epochSecond}")
        client.putItem { it.tableName(table).item(item + key) }
    }

    suspend fun get(key: Key) = withContext(IO) { client.getItem { it.tableName(table).key(key) } }

    suspend fun update(
        key: Key,
        block: Item.() -> Unit = {},
    ) = withContext(IO) {
        val item = mutableMapOf<String, AttributeValue>().apply(block)
        item["modified"] = fromN("${now().epochSecond}")

        val (names, values) = item.entries.mapIndexed { i, (name, value) -> ("#n$i" to name) to (":v$i" to value) }.unzip()
        val (nameExpressions) = names.unzip()
        val (valueExpressions) = values.unzip()

        val expressions = nameExpressions.zip(valueExpressions)
        val expression = expressions.joinToString(", ") { (name, value) -> "$name = $value" }

        client.updateItem {
            it.tableName(table).key(key)
                .updateExpression("set $expression")
                .expressionAttributeValues(values.toMap())
                .expressionAttributeNames(names.toMap())
                .returnValues(ReturnValue.ALL_NEW)
        }
    }

    suspend fun delete(key: Key) = withContext(IO) { client.deleteItem { it.tableName(table).key(key) } }
}
