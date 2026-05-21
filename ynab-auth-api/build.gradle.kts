plugins {
    id("kotlin-jvm")
}

group = "noodle.ynab"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation("io.ktor:ktor-client-auth")
    implementation(libs.bundles.ktor.client)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
