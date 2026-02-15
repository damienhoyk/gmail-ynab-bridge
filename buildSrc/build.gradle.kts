plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.graalvm.native.plugin)
    implementation(libs.kotlin.gradle)
}
