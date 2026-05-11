plugins {
    id("kotlin-jvm")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":security"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
}
