plugins {
    id("kotlin-jvm")
    id("kotlin-native-test")
    alias(libs.plugins.kotlin.serialization)
}

group = "noodle.oauth"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":oauth"))
    implementation(project(":oauth2-api"))
    implementation(project(":oauth-api"))
    implementation(project(":ynab-auth-api"))
    implementation(project(":ynab-api"))
    implementation(libs.bundles.ktor.client)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}

graalvmNative {
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/noodle.oauth/oauth-ynab-api"),
            )
        }
    }
}
