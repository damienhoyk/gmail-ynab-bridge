plugins {
	alias(libs.plugins.kotlin.serialization)
	id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.finance.budget.bridge"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
	implementation(platform(libs.ktor.dependencies))
    implementation(project(":bridge-repository"))
    implementation(project(":gmail-ynab-job"))
    implementation(project(":google-auth"))
    implementation(project(":google-gmail"))
    implementation(project(":security"))
    implementation(project(":security-repository"))
    implementation(project(":ynab"))
    implementation(libs.bundles.aws.lambda)
	implementation(libs.kotlinx.coroutines)
    implementation("com.bitwarden:sdk-secrets:1.0.1")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-core")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
	runtimeOnly(libs.logback)
}
