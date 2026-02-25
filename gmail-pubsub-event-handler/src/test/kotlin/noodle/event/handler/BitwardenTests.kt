package noodle.event.handler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import noodle.home.security.bitwardenClient
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class BitwardenTests {

    @Test
    fun test() = runBlocking {
        val t = measureTimeMillis {
            val job1 = async(Default) {
                println(Thread.currentThread().name)
                bitwardenClient()
            }
            val job2 = async(Default) {
                println(Thread.currentThread().name)
                Thread.sleep(1000)
            }
            listOf(job1, job2).awaitAll()
        }
        println(t)
    }

    @Test
    fun test2() = runBlocking {
        val t = measureTimeMillis {
            val job1 = async(Default) {
                println(Thread.currentThread().name)
                bitwardenClient()
            }
            println(Thread.currentThread().name)
            Thread.sleep(1000)
            job1.await()
        }
        println(t)
    }
}