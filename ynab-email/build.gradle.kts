plugins {
	id("kotlin-jvm")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
	implementation(project(":ynab"))
	implementation(libs.jackson.kotlin)
	implementation(libs.jackson.yaml)
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testImplementation("org.junit.jupiter:junit-jupiter-params")
}
