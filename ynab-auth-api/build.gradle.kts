plugins {
    id("kotlin-jvm")
}

group = "noodle.ynab"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":oauth"))
    implementation(project(":oauth-api"))
    implementation("io.ktor:ktor-client-auth")
    implementation(libs.bundles.ktor.client)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
