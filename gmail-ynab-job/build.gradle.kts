plugins {
	alias(libs.plugins.kotlin.serialization)
	id("kotlin-jvm")
}

group = "noodle.home.gmail.ynab.job"
version = "0.0.1-SNAPSHOT"

dependencies {
	implementation(platform(libs.ktor.dependencies))
	implementation(project(":google-gmail"))
	implementation(project(":google-auth"))
	implementation(project(":ynab"))
	implementation(project(":security"))
	implementation(libs.jackson.kotlin)
	implementation(libs.jackson.yaml)
	implementation(libs.kotlinx.coroutines)
	implementation("io.ktor:ktor-client-auth")
	implementation("io.ktor:ktor-client-core")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testImplementation("org.junit.jupiter:junit-jupiter-params")
}
