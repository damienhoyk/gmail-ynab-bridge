plugins {
    id("kotlin-jvm")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":ynab"))
    implementation(project(":google-gmail"))
    implementation(project(":security"))
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
    implementation(libs.jakarta.mail)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}
