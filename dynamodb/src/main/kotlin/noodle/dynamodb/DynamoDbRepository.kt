package noodle.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

abstract class DynamoDbRepository(
    private val environment: String? = null,
) : DynamoDbCrud() {
    abstract val name: String
    abstract val partitionKey: String
    override val table: String get() = environment?.let { "$name-$it" } ?: name

    suspend fun put(
        partition: String,
        block: Item.() -> Unit = {},
    ) = put(key(partition), block)

    suspend fun get(partition: String) = get(key(partition))

    suspend fun update(
        partition: String,
        block: Item.() -> Unit = {},
    ) = update(key(partition), block)

    suspend fun delete(partition: String) = delete(key(partition))

    protected fun key(partition: String) = mapOf(partitionKey to fromS(partition))
}
