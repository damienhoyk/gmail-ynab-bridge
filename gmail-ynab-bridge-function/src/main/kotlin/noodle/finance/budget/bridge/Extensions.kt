package noodle.finance.budget.bridge

import kotlinx.coroutines.coroutineScope
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import java.util.function.Consumer

suspend fun DynamoDbClient.getItemAsync(request: Consumer<GetItemRequest.Builder>) = coroutineScope { getItem(request) }
suspend fun DynamoDbClient.queryAsync(request: Consumer<QueryRequest.Builder>) = coroutineScope { query(request) }
suspend fun DynamoDbClient.scanAsync(request: Consumer<ScanRequest.Builder>) = coroutineScope { scan(request) }
suspend fun DynamoDbClient.updateItemAsync(request: Consumer<UpdateItemRequest.Builder>) = coroutineScope { updateItem(request) }
