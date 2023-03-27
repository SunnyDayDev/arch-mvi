plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.jdkVersion.get().toInt())
}

dependencies {
    implementation(project(":mvi-core-primitives"))
    implementation(project(":mvi-core"))
    implementation(project(":mvi-test-util"))

    implementation(libs.coroutines)
    implementation(libs.test.coroutines)
    implementation(libs.test.kotlin)
    implementation(libs.test.junit5)
    implementation(libs.test.mockk)
}