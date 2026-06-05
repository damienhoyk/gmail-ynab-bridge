plugins {
    id("kotlin-jvm")
}

group = "noodle.ynab.auth"

version = "0.0.1-SNAPSHOT"

dependencies {
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
