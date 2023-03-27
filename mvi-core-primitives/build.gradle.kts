plugins {
    alias(libs.plugins.kotlin.jvm)
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