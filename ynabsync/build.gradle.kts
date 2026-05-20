plugins {
    id("kotlin-jvm")
}

group = "noodle.ynabsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}
