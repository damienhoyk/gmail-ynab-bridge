plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
    id("kotlin-native-test")
}

group = "noodle.oauth"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":serialization"))
    implementation(project(":dynamodb"))
    implementation(project(":oauth"))
    implementation(project(":oauth-api"))
    implementation(project(":oauth-persistence"))
    implementation(project(":ynabsync"))
    implementation(project(":ynab-api"))
    implementation(project(":ynab-auth-api"))
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
