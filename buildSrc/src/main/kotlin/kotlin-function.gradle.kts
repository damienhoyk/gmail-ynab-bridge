plugins {
    kotlin("jvm")
}

tasks.register<Zip>("packageFunction") {
    into("lib") {
        from(tasks.named("jar"))
    }
}

tasks.register<Zip>("packageLayer") {
    destinationDirectory = layout.buildDirectory.dir("distributions")
    archiveFileName = "layer.zip"
    exclude("**/sdk-secrets-*.jar")
    into("java/lib") {
        from(configurations.runtimeClasspath)
    }
}

tasks.build {
    dependsOn(tasks.named("packageFunction"))
    dependsOn(tasks.named("packageLayer"))
}
