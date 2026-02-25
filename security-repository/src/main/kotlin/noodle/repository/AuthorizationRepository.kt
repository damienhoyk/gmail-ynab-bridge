package noodle.repository

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class AuthorizationRepository(
    private val table: String = "authorization",
    private val client: DynamoDbClient = DynamoDbClient.create()
) {

    fun get(id: String?) = client.getItem {
        val key = mapOf("id" to fromS(id))
        it.tableName(table).key(key)
    }

}