plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-native-test")
}

group = "noodle.oauth"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden-api"))
    implementation(project(":ktor"))
    implementation(project(":oauth"))
    implementation(project(":oauth2-api"))
    implementation("io.ktor:ktor-client-auth")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation(libs.bundles.ktor.client)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}

graalvmNative {
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/noodle.oauth/oauth"),
            )
        }
    }
}
