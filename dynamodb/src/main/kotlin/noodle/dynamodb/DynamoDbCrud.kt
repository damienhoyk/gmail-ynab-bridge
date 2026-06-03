package noodle.dynamodb

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.ReturnValue
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse
import java.time.Instant.now

public abstract class DynamoDbCrud {
    public typealias Item = MutableMap<String, AttributeValue>
    public typealias Key = Map<String, AttributeValue>

    protected abstract val client: DynamoDbClient

    public abstract val table: String

    public suspend fun put(
        key: Key,
        block: Item.() -> Unit = {},
    ): PutItemResponse =
        withContext(IO) {
            val item = mutableMapOf<String, AttributeValue>().apply(block)
            item["modified"] = fromN("${now().epochSecond}")
            client.putItem { it.tableName(table).item(item + key) }
        }

    public suspend fun get(key: Key): GetItemResponse =
        withContext(IO) {
            client.getItem { it.tableName(table).key(key) }
        }

    public suspend fun update(
        key: Key,
        block: Item.() -> Unit = {},
    ): UpdateItemResponse =
        withContext(IO) {
            val item = mutableMapOf<String, AttributeValue>().apply(block)
            item["modified"] = fromN("${now().epochSecond}")

            val (names, values) = item.entries.mapIndexed { i, (name, value) -> ("#n$i" to name) to (":v$i" to value) }.unzip()
            val (nameExpressions) = names.unzip()
            val (valueExpressions) = values.unzip()

            val expressions = nameExpressions.zip(valueExpressions)
            val expression = expressions.joinToString(", ") { (name, value) -> "$name = $value" }

            client.updateItem {
                it
                    .tableName(table)
                    .key(key)
                    .updateExpression("set $expression")
                    .expressionAttributeValues(values.toMap())
                    .expressionAttributeNames(names.toMap())
                    .returnValues(ReturnValue.ALL_NEW)
            }
        }

    public suspend fun delete(key: Key): DeleteItemResponse =
        withContext(IO) {
            client.deleteItem { it.tableName(table).key(key) }
        }
}
