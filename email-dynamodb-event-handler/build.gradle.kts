plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.email.handler"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bridge-repository"))
    implementation(project(":email-repository"))
    implementation(project(":google-gmail"))
    implementation(project(":google-auth"))
    implementation(project(":gmail-ynab-job"))
    implementation(project(":security"))
    implementation(project(":ynab"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.jakarta.mail)
    implementation(libs.kotlinx.coroutines)
    implementation("com.bitwarden:sdk-secrets:1.0.1")
    implementation("joda-time:joda-time:2.14.0")
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.797")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}