plugins {
    id("kotlin-jvm")
}

group = "noodle.gmailsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
}
