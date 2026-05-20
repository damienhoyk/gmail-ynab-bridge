package noodle.email.infrastructure.handler

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

// Guards reflect-config entries for APIGatewayV2HTTPEvent and all nested classes.
// Lambda RIC deserializes the full event graph before the handler is called, so
// RequestContext, Authorizer, JWT, and Http must be instantiable via reflection even
// when handlers only read body/headers/queryStringParameters.
private val serializer =
    LambdaEventSerializers.serializerFor(APIGatewayV2HTTPEvent::class.java, APIGatewayV2HTTPEvent::class.java.classLoader)

class LambdaEventDeserializationTests {
    @Test
    fun apiGatewayEventWithBody() {
        val raw = """{"version":"2.0","rawPath":"/pubsub","headers":{"authorization":"Bearer tok"},"requestContext":{"accountId":"123","apiId":"api1","http":{"method":"POST","path":"/pubsub","protocol":"HTTP/1.1","sourceIp":"1.2.3.4","userAgent":"agent"},"requestId":"r1","routeKey":"POST /pubsub","stage":"${"$"}default","timeEpoch":1747040400000},"body":"{\"key\":\"value\"}","isBase64Encoded":false}"""

        val result = serializer.fromJson(raw)

        assertEquals("Bearer tok", result.headers["authorization"])
        assertEquals("{\"key\":\"value\"}", result.body)
        assertNotNull(result.requestContext)
        assertEquals("POST", result.requestContext.http.method)
        assertEquals("1.2.3.4", result.requestContext.http.sourceIp)
    }

    @Test
    fun apiGatewayEventWithJwtAuthorizer() {
        val raw = """{"version":"2.0","rawPath":"/pubsub","headers":{},"requestContext":{"accountId":"123","apiId":"api1","authorizer":{"jwt":{"claims":{"sub":"user-123","iss":"https://accounts.google.com"},"scopes":["openid","email"]}},"http":{"method":"POST","path":"/pubsub","protocol":"HTTP/1.1","sourceIp":"5.6.7.8","userAgent":"agent"},"requestId":"r2","routeKey":"POST /pubsub","stage":"${"$"}default","timeEpoch":1747040400000},"isBase64Encoded":false}"""

        val result = serializer.fromJson(raw)

        assertNotNull(result.requestContext.authorizer)
        assertNotNull(result.requestContext.authorizer.jwt)
        assertEquals("user-123", result.requestContext.authorizer.jwt.claims["sub"])
    }

    @Test
    fun apiGatewayEventWithQueryStringParameters() {
        val raw = """{"version":"2.0","rawPath":"/callback","headers":{},"queryStringParameters":{"code":"auth-code","state":"random-state"},"requestContext":{"accountId":"123","apiId":"api1","http":{"method":"GET","path":"/callback","protocol":"HTTP/1.1","sourceIp":"1.2.3.4","userAgent":"Mozilla"},"requestId":"r3","routeKey":"GET /callback","stage":"${"$"}default","timeEpoch":1747040400000},"isBase64Encoded":false}"""

        val result = serializer.fromJson(raw)

        assertEquals("auth-code", result.queryStringParameters["code"])
        assertEquals("random-state", result.queryStringParameters["state"])
    }
}
