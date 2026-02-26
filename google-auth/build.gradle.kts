plugins {
	alias(libs.plugins.kotlin.serialization)
	id("kotlin-jvm")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
	implementation(platform(libs.ktor.dependencies))
	implementation(project(":security"))
	implementation(libs.bundles.ktor.client)
	implementation(libs.kotlinx.coroutines)
}
