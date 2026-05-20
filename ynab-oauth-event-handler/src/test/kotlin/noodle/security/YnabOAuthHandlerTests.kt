package noodle.security

import noodle.security.infrastructure.handler.YnabOAuthHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class YnabOAuthHandlerTests {
    @Test
    fun initTime() {
        val timeInMillis =
            measureTimeMillis {
                YnabOAuthHandler()
            }

        println("Init time: $timeInMillis ms")
        Assertions.assertTrue(timeInMillis < 5000) { "Init took too long: $timeInMillis ms" }
    }

    @Disabled
    @Test
    fun handleRequest() {
        val handler = YnabOAuthHandler()
        TODO("Not yet implemented")
    }
}
