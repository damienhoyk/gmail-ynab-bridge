package noodle.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse

public abstract class DynamoDbRepository(
    private val environment: String? = null,
) : DynamoDbCrud() {
    public abstract val name: String
    public abstract val partitionKey: String
    override val table: String get() = environment?.let { "$name-$it" } ?: name

    public suspend fun put(
        partition: String,
        block: Item.() -> Unit = {},
    ): PutItemResponse = put(key(partition), block)

    public suspend fun get(partition: String): GetItemResponse = get(key(partition))

    public suspend fun update(
        partition: String,
        block: Item.() -> Unit = {},
    ): UpdateItemResponse = update(key(partition), block)

    public suspend fun delete(partition: String): DeleteItemResponse = delete(key(partition))

    protected fun key(partition: String): Key = mapOf(partitionKey to fromS(partition))
}
