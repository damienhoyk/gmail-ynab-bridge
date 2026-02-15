plugins {
    id("kotlin-jvm")
    application
}

application {
    mainClass = "noodle.finance.account.ApplicationKt"
}

group = "noodle.finance.account"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation(project(":google-auth"))
    implementation(project(":google-gmail"))
    implementation(project(":gmail-ynab-job"))
    implementation(project(":security"))
    implementation(project(":ynab"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.logback)
}