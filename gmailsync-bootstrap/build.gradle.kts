plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.gmailsync"
version = "0.0.1-SNAPSHOT"

graalvmNative {
    testSupport = true // kotlin-function-native does not set this; must be explicit
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/com.aws/aws-lambda-java-events"),
                rootProject.file("META-INF/native-image/com.aws/aws-lambda-java-serialization"),
                rootProject.file("META-INF/native-image/org.joda/joda-time"),
            )
            buildArgs.addAll(listOf("--initialize-at-build-time=kotlin,kotlinx.serialization,kotlinx.coroutines"))
        }
    }
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":serialization"))
    implementation(project(":dynamodb"))
    implementation(project(":gmailsync"))
    implementation(project(":gmailsync-api"))
    implementation(project(":gmailsync-persistence"))
    implementation(project(":gmail-api"))
    implementation(project(":google-auth-api"))
    implementation(project(":oauth"))
    implementation(project(":oauth-google-api"))
    implementation(project(":oauth-api"))
    implementation(project(":oauth-persistence"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}
