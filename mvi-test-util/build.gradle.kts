plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.jdkVersion.get().toInt())

    sourceSets {
        main.configure {
            kotlin.srcDirs("src/main/java", "src/main/kotlin")
            resources.srcDirs("src/main/resources")
        }
    }
}

dependencies {
    implementation(project(":mvi-core-primitives"))
    implementation(libs.coroutines)

    implementation(libs.test.coroutines)
    implementation(libs.test.kotlin)
    implementation(libs.test.junit5)
    implementation(libs.test.mockk)
}