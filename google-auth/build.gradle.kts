plugins {
	alias(libs.plugins.kotlin.serialization)
	id("kotlin-jvm")
}

dependencies {
	implementation(platform(libs.ktor.dependencies))
	implementation(project(":security"))
	implementation(libs.bundles.ktor.client)
	implementation(libs.kotlinx.coroutines)
	implementation(project(":security"))
}
