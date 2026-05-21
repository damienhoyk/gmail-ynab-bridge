package noodle.dynamodb

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

abstract class DynamoDbSortRepository : DynamoDbCrud() {
    abstract val name: String

    abstract val partitionKey: String
    abstract val sortKey: String

    suspend fun put(
        partition: String,
        sort: String,
        block: Item.() -> Unit = {},
    ) = put(key(partition, sort), block)

    suspend fun get(
        partition: String,
        sort: String,
    ) = get(key(partition, sort))

    suspend fun query(partition: String) =
        withContext(IO) {
            client.query {
                it.tableName(table)
                    .expressionAttributeNames(mapOf("#s" to partitionKey))
                    .expressionAttributeValues(mapOf(":s" to fromS(partition)))
                    .keyConditionExpression("#s = :s")
            }
        }

    suspend fun update(
        partition: String,
        sort: String,
        block: Item.() -> Unit = {},
    ) = update(key(partition, sort), block)

    suspend fun delete(
        partition: String,
        sort: String,
    ) = delete(key(partition, sort))

    protected fun key(
        partition: String,
        sort: String,
    ) = mapOf(partitionKey to fromS(partition), sortKey to fromS(sort))
}
