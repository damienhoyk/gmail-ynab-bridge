package noodle.event.handler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import kotlin.system.measureTimeMillis

class TelegramBotHandlerTests {

    @Test
    fun initTime() {
        val timeInMillis = measureTimeMillis {
            TelegramBotHandler()
        }
        
        println("Init time: $timeInMillis ms")
        assertTrue(timeInMillis < 5000) { "Init took too long: $timeInMillis ms" }
    }

    @Disabled
    @Test
    fun handleRequest() {
        val handler = TelegramBotHandler()
        TODO("Not yet implemented")
    }

}
