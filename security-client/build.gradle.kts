plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-native-test")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":security-bitwarden"))
    implementation(project(":security"))
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
                rootProject.file("META-INF/native-image/noodle.security/security"),
            )
        }
    }
}
