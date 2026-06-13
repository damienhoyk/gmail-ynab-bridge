plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-native-test")
}

group = "noodle"

version = "0.0.1-SNAPSHOT"

graalvmNative {
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/noodle.oauth2/oauth2-api"),
            )
        }
    }
}

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":tokenrefresher"))
    implementation(project(":oauth2-api"))
    implementation(project(":ktor"))
    implementation(libs.bundles.ktor.client)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
