plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.ynabsync"
version = "0.0.1-SNAPSHOT"

graalvmNative {
    testSupport = true // kotlin-function-native does not set this; must be explicit
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/com.aws/aws-lambda-java-events"),
                rootProject.file("META-INF/native-image/com.aws/aws-lambda-java-serialization"),
                rootProject.file("META-INF/native-image/com.aws/aws-java-sdk-dynamodb"),
                rootProject.file("META-INF/native-image/org.joda/joda-time"),
            )
            buildArgs.addAll(listOf("--initialize-at-build-time=kotlin,kotlinx.serialization,kotlinx.coroutines"))
        }
    }
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":security-bitwarden"))
    implementation(project(":bridge-dynamodb"))
    implementation(project(":dynamodb"))
    implementation(project(":email-dynamodb"))
    implementation(project(":ynab-client"))
    implementation(project(":google-auth-client"))
    implementation(project(":gmailsync"))
    implementation(project(":google-gmail-client"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":security-dynamodb"))
    implementation(project(":ynabsync"))
    implementation(project(":ynab-auth-client"))
    implementation(project(":ynabsync"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bitwarden.secrets)
    implementation(libs.jakarta.mail)
    implementation(libs.kotlinx.coroutines)
    implementation("io.ktor:ktor-client-auth")
    implementation("joda-time:joda-time:2.14.0")
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.797")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}
