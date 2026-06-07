plugins {
    id("org.graalvm.buildtools.native")
    kotlin("jvm")
    application
}

application { mainClass = "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" }

graalvmNative {
    binaries {
        named("main") {
            imageName = "function"

            configurationFileDirectories.from(
                    file("../META-INF/native-image/com.aws/aws-java-sdk-dynamodb"),
                    file("../META-INF/native-image/com.aws/aws-lambda-java-core"),
                    file("../META-INF/native-image/com.aws/aws-lambda-java-events"),
                    file("../META-INF/native-image/com.aws/aws-lambda-java-runtime-interface-client"),
                    file("../META-INF/native-image/com.aws/aws-lambda-java-serialization"),
                    file("../META-INF/native-image/com.bitwarden/sdk-secrets"),
                    file("../META-INF/native-image/io.ktor/ktor-client-content-negotiation"),
                    file("../META-INF/native-image/io.ktor/ktor-client-core"),
                    file("../META-INF/native-image/noodle.gmail/gmail-api"),
                    file("../META-INF/native-image/noodle.ynab/ynab-api"),
                    file("../META-INF/native-image/noodle.google.auth/google-auth-api"),
                    file("../META-INF/native-image/noodle.oauth2/oauth2-api"),
                    file("../META-INF/native-image/org.joda/joda-time")
            )

            buildArgs.addAll(
                    listOf(
                            "--initialize-at-build-time=io.ktor,kotlin",
                            "--initialize-at-build-time=kotlinx.coroutines",
                            "--initialize-at-run-time=io.ktor.client,io.ktor.http,io.ktor.serialization.kotlinx",
                            "--enable-http"
                    )
            )
        }
    }
}

tasks.register<Zip>("packageFunction") {
    archiveFileName = "${project.name}-$version-native.zip"
    into("/") {
        from("../buildSrc/src/main/resources/bootstrap") { filePermissions { unix("777") } }
        from(tasks.named("nativeCompile"))
    }
}
