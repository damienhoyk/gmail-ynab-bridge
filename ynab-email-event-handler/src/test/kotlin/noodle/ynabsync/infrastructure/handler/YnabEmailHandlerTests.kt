package noodle.finance.infrastructure.handler

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class YnabEmailHandlerTests {
    @Test
    fun initTime() {
        val timeInMillis =
            measureTimeMillis {
                YnabEmailHandler()
            }

        println("Init time: $timeInMillis ms")
        Assertions.assertTrue(timeInMillis < 5000) { "Init took too long: $timeInMillis ms" }
    }

    @Disabled
    @Test
    fun handleRequest() {
        val handler = YnabEmailHandler()
        TODO("Not yet implemented")
    }
}
