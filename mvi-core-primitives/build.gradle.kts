plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(libs.versions.jdkVersion.get().toInt())
}

dependencies {
    testImplementation(project(":mvi-test-util"))
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.kotlin)
    testImplementation(libs.test.junit5)
    testImplementation(libs.test.mockk)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.sunnyday.arch-mvi"
            artifactId = "mvi-core-primitives"
            version = "0.1"

            from(components["kotlin"])
        }
    }
}