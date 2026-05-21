plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-native-test")
}

group = "noodle.gmailsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":gmailsync"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":google-auth-api"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.jakarta.mail)
    implementation("io.ktor:ktor-client-auth")
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}

graalvmNative {
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/noodle.gmailsync/gmailsync"),
            )
        }
    }
}
