package noodle.home.gmail.ynab.job

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import noodle.ynab.Transaction
import java.io.File
import java.net.URL
import kotlin.io.path.toPath

fun jacksonObjectMapper(jf: JsonFactory) = ObjectMapper(jf).registerKotlinModule()
fun getResource(name: String) = {}.javaClass.getResource(name)

fun List<TransactionMatcher>.parse(message: String) = firstNotNullOfOrNull { matcher -> matcher.parse(message) }
fun List<TransactionMatcher>.parse(messages: List<String>): Pair<List<Transaction>, List<String>> {
    val (parsed, unparsed) = messages
        .associateWith { parse(it) }
        .entries.partition { it.value != null }

    return parsed.map { it.value!! } to unparsed.map { it.key }
}

inline fun <reified T> File.readYaml() = readText().let { jacksonObjectMapper(YAMLFactory()).readValue<T>(it) }
inline fun <reified T> URL.readYaml() = toURI().toPath().toFile().readYaml<T>()
