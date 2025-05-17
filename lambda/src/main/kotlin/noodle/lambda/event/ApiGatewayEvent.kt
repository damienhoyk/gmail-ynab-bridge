package noodle.lambda.event

data class ApiGatewayEvent(
    var headers: Map<String, String>? = null,
    var body: String? = null,
    var queryStringParameters: Map<String, String> = emptyMap(),
    var rawPath: String? = null,
    var rawQueryString: String? = null
)