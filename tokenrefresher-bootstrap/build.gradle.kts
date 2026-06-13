plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
    id("kotlin-native-test")
}

group = "noodle.tokenrefresher"
version = "0.0.1-SNAPSHOT"

graalvmNative {
    testSupport = true // kotlin-function-native does not set this; must be explicit
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/com.aws/aws-lambda-java-events"),
                rootProject.file("META-INF/native-image/com.aws/aws-lambda-java-serialization"),
                rootProject.file("META-INF/native-image/com.aws/aws-java-sdk-dynamodb"),
            )
            buildArgs.addAll(listOf("--initialize-at-build-time=kotlin,kotlinx.serialization,kotlinx.coroutines"))
        }
    }
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden-api"))
    implementation(project(":tokenrefresher"))
    implementation(project(":tokenrefresher-google-api"))
    implementation(project(":tokenrefresher-ynab-api"))
    implementation(project(":tokenrefresher-persistence"))
    implementation(project(":oauth2-api"))
    implementation(project(":ynab-auth-api"))
    implementation(project(":dynamodb"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}
