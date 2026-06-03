package noodle.ynabsync.core.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.net.URL
import kotlin.io.path.toPath

internal fun jacksonObjectMapper(jf: JsonFactory): ObjectMapper = ObjectMapper(jf).registerKotlinModule()

internal fun getResource(name: String) = {}.javaClass.getResource(name)

internal inline fun <reified T> File.readYaml(): T = readText().let { jacksonObjectMapper(YAMLFactory()).readValue<T>(it) }

internal inline fun <reified T> URL.readYaml(): T = toURI().toPath().toFile().readYaml<T>()
