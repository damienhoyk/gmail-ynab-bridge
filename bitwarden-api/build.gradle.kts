plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-native-test")
}

graalvmNative {
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/com.bitwarden/sdk-secrets"),
            )
        }
    }
}

group = "noodle.bitwarden"
version = "0.0.1-SNAPSHOT"

kotlin {
    explicitApi()
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.bitwarden.secrets)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:secretsmanager")
    testImplementation("software.amazon.awssdk:signin")
}
