plugins {
	alias(libs.plugins.kotlin.serialization)
	id("kotlin-jvm")
}

group = "noodle.repository"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
	implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
	runtimeOnly(libs.logback)
}
