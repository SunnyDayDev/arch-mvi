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
    implementation(project(":mvi-core-primitives"))
    implementation(project(":mvi-core"))

    implementation(libs.coroutines)

    testImplementation(project(":mvi-test-util"))
    testImplementation(project(":mvi-test-common"))
    testImplementation(libs.test.kotlin)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.junit5)
    testImplementation(libs.test.mockk)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.sunnyday.arch-mvi"
            artifactId = "mvi-kit-coroutine"
            version = "0.1"

            from(components["kotlin"])
        }
    }
}