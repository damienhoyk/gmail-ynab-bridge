plugins {
	alias(libs.plugins.kotlin.serialization)
	id("kotlin-jvm")
    id("kotlin-function")
}

group = "noodle.finance.budget.bridge"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
	implementation(platform(libs.ktor.dependencies))
    implementation(project(":gmail-ynab-job"))
    implementation(project(":google-auth"))
    implementation(project(":google-gmail"))
    implementation(project(":security"))
    implementation(project(":ynab"))
    implementation(libs.aws.lambda.core)
    implementation(libs.aws.lambda.events)
    implementation(libs.jackson.kotlin)
	implementation(libs.kotlinx.coroutines)
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-core")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
	runtimeOnly(libs.logback)
}
