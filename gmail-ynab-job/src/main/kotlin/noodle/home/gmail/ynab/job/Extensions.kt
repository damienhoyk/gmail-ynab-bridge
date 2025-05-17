package noodle.home.gmail.ynab.job

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.net.URL
import kotlin.io.path.toPath

fun jacksonObjectMapper(jf: JsonFactory) = ObjectMapper(jf).registerKotlinModule()
fun getResource(name: String) = {}.javaClass.getResource(name)

inline fun <reified T> File.readYaml() = readText().let { jacksonObjectMapper(YAMLFactory()).readValue<T>(it) }
inline fun <reified T> URL.readYaml() = toURI().toPath().toFile().readYaml<T>()
