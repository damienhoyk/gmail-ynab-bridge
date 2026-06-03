package noodle.dynamodb

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse

public abstract class DynamoDbSortRepository(
    private val environment: String? = null,
) : DynamoDbCrud() {
    public abstract val name: String

    public abstract val partitionKey: String
    public abstract val sortKey: String
    override val table: String get() = environment?.let { "$name-$it" } ?: name

    public suspend fun put(
        partition: String,
        sort: String,
        block: Item.() -> Unit = {},
    ): PutItemResponse = put(key(partition, sort), block)

    public suspend fun get(
        partition: String,
        sort: String,
    ): GetItemResponse = get(key(partition, sort))

    public suspend fun query(partition: String): QueryResponse =
        withContext(IO) {
            client.query {
                it
                    .tableName(table)
                    .expressionAttributeNames(mapOf("#s" to partitionKey))
                    .expressionAttributeValues(mapOf(":s" to fromS(partition)))
                    .keyConditionExpression("#s = :s")
            }
        }

    public suspend fun update(
        partition: String,
        sort: String,
        block: Item.() -> Unit = {},
    ): UpdateItemResponse = update(key(partition, sort), block)

    public suspend fun delete(
        partition: String,
        sort: String,
    ): DeleteItemResponse = delete(key(partition, sort))

    protected fun key(
        partition: String,
        sort: String,
    ): Key = mapOf(partitionKey to fromS(partition), sortKey to fromS(sort))
}
