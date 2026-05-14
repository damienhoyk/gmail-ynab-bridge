plugins {
    id("org.graalvm.buildtools.native")
}

graalvmNative {
    testSupport = true
    binaries {
        named("test") {
            configurationFileDirectories.from(
                rootProject.file("META-INF/native-image/io.ktor/ktor-client-core"),
            )
            buildArgs.addAll(listOf("--initialize-at-build-time=kotlin,kotlinx.serialization"))
        }
    }
}
