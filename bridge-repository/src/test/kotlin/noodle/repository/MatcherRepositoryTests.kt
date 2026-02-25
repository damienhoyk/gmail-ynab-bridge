package noodle.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class MatcherRepositoryTests {

    val repository = MatcherRepository()

    @Test
    fun query(): Unit = runBlocking {
        val results = repository.query("unialerts@uobgroup.com")
        println(results.items())
    }
}