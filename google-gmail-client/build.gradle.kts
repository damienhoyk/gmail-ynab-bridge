plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-native-test")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":google-gmail"))
    implementation(project(":gmail-pubsub"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":telegram-bot"))
    implementation(project(":ynab-email"))
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
                rootProject.file("META-INF/native-image/noodle.email/google-gmail"),
            )
        }
    }
}
