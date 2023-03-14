plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "dev.sunnyday"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.coroutines)
    testImplementation(libs.test.kotlin)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.junit5)
    testImplementation(libs.test.mockk)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.sunnyday"
            artifactId = "arch-mvi"
            version = "0.1"

            from(components["kotlin"])
        }
    }
}