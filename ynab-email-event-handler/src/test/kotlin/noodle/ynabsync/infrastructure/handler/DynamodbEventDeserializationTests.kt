package noodle.finance.infrastructure.handler

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

// Guards reflect-config entries for DynamodbEvent, DynamodbStreamRecord,
// models.dynamodb.{StreamRecord,AttributeValue,Record,Identity}, v1 SDK
// {StreamRecord,AttributeValue}, and org.joda.time.DateTime.
// LambdaEventSerializers.serializerFor applies the DynamodbEventMixin and DateTimeModule
// from aws-lambda-java-serialization — the same shaded Jackson path Lambda RIC uses.
// Removal of the DateTime reflect-config entry causes ClassNotFoundException at runtime
// because DynamoDB Streams always populates approximateCreationDateTime.
private val serializer =
    LambdaEventSerializers.serializerFor(DynamodbEvent::class.java, DynamodbEvent::class.java.classLoader)

class DynamodbEventDeserializationTests {
    @Test
    fun deserializesInsertWithNewImage() {
        // DynamoDB Streams JSON uses PascalCase inside the "dynamodb" block (Keys, NewImage, etc.)
        val raw = """{"Records":[{"eventID":"1","eventVersion":"1.1","dynamodb":{"Keys":{"id":{"S":"123"}},"NewImage":{"destination":{"S":"user@app.ynab.com"},"source":{"S":"msg-id:user@gmail.com"}},"ApproximateCreationDateTime":1747040400,"SequenceNumber":"111","SizeBytes":26,"StreamViewType":"NEW_IMAGE"},"awsRegion":"ap-southeast-1","eventName":"INSERT","eventSourceARN":"arn:aws:dynamodb:ap-southeast-1:123:table/outbox/stream/2026-05-12T00:00:00.000","eventSource":"aws:dynamodb"}]}"""

        val result = serializer.fromJson(raw)

        assertEquals(1, result.records.size)
        val record = result.records.first()
        assertEquals("INSERT", record.eventName)
        assertEquals("user@app.ynab.com", record.dynamodb.newImage["destination"]?.s)
        assertEquals("msg-id:user@gmail.com", record.dynamodb.newImage["source"]?.s)
        // Assert DateTime type and millis value — a missing DateTime reflect-config entry causes
        // ClassNotFoundException in the shaded Jackson DateTimeModule deserializer.
        val ts: java.util.Date = record.dynamodb.approximateCreationDateTime
        assertEquals(1747040400_000L, ts.time)
    }

    @Test
    fun deserializesModifyRecord() {
        val raw = """{"Records":[{"eventID":"2","eventVersion":"1.1","dynamodb":{"Keys":{"id":{"S":"456"}},"NewImage":{"destination":{"S":"other@app.ynab.com"}},"ApproximateCreationDateTime":1747040401,"SequenceNumber":"222","SizeBytes":20,"StreamViewType":"NEW_IMAGE"},"awsRegion":"ap-southeast-1","eventName":"MODIFY","eventSourceARN":"arn:aws:dynamodb:ap-southeast-1:123:table/outbox/stream/2026-05-12T00:00:00.000","eventSource":"aws:dynamodb"}]}"""

        val result = serializer.fromJson(raw)

        assertEquals(1, result.records.size)
        assertEquals("MODIFY", result.records.first().eventName)
        val inserts = result.records.filter { "insert".equals(it.eventName, ignoreCase = true) }
        assertEquals(0, inserts.size)
    }

    @Test
    fun deserializesAttributeValueTypes() {
        val raw = """{"Records":[{"eventID":"3","eventVersion":"1.1","dynamodb":{"Keys":{"id":{"S":"789"}},"NewImage":{"strField":{"S":"hello"},"numField":{"N":"42"},"boolField":{"BOOL":true},"nullField":{"NULL":true}},"ApproximateCreationDateTime":1747040402,"SequenceNumber":"333","SizeBytes":50,"StreamViewType":"NEW_IMAGE"},"awsRegion":"ap-southeast-1","eventName":"INSERT","eventSourceARN":"arn:aws:dynamodb:ap-southeast-1:123:table/outbox/stream/2026-05-12T00:00:00.000","eventSource":"aws:dynamodb"}]}"""

        val result = serializer.fromJson(raw)

        val image = result.records.first().dynamodb.newImage
        assertEquals("hello", image["strField"]?.s)
        assertEquals("42", image["numField"]?.n)
        assertNotNull(image["boolField"]?.getBOOL())
        assertNotNull(image["nullField"]?.getNULL())
    }
}
