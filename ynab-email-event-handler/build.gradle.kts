plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.finance"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":bridge-dynamodb"))
    implementation(project(":dynamodb"))
    implementation(project(":email-dynamodb"))
    implementation(project(":ynab-client"))
    implementation(project(":google-auth-client"))
    implementation(project(":google-gmail"))
    implementation(project(":google-gmail-client"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":security-dynamodb"))
    implementation(project(":ynab"))
    implementation(project(":ynab-auth-client"))
    implementation(project(":ynab-email"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bitwarden.secrets)
    implementation(libs.jakarta.mail)
    implementation(libs.kotlinx.coroutines)
    implementation("joda-time:joda-time:2.14.0")
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.797")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}
