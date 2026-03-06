plugins {
    `java-library`
}

group = "io.github.paulem"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Retrofit2 for HTTP client
    api("com.squareup.retrofit2:retrofit:2.11.0")
    api("com.squareup.retrofit2:converter-jackson:2.11.0")

    // Jackson for JSON serialization/deserialization
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.2")

    // OkHttp for SSE streaming and HTTP
    api("com.squareup.okhttp3:okhttp:4.12.0")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}
