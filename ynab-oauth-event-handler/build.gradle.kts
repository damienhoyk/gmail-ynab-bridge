plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":dynamodb"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":security-dynamodb"))
    implementation(project(":user-dynamodb"))
    implementation(project(":ynabsync"))
    implementation(project(":ynab-client"))
    implementation(project(":ynab-auth-client"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
    testImplementation("software.amazon.awssdk:signin")
}

tasks.withType<Test>().configureEach {
    environment("REDIRECT_URI", "http://localhost")
    environment("SECRET_ID", "test")
}
