plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":bridge-repository"))
    implementation(project(":dynamodb"))
    implementation(project(":email-repository"))
    implementation(project(":google-gmail"))
    implementation(project(":google-client-factory"))
    implementation(project(":google-auth"))
    implementation(project(":security"))
    implementation(project(":security-repository"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bitwarden.secrets)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}