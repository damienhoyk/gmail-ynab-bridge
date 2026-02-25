package noodle.repository

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class MatcherRepository(
    val client: DynamoDbClient = DynamoDbClient.create(),
) {

    val table = "gmail-ynab-bridge-matcher"

    suspend fun query(source: String) = withContext(IO) {
        client.query {
            it.tableName(table)
                .expressionAttributeNames(mapOf("#s" to "source"))
                .expressionAttributeValues(mapOf(":s" to fromS(source)))
                .keyConditionExpression("#s = :s")
        }
    }

}