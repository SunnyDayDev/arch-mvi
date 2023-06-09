pluginManagement {
    repositories {
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "arch-mvi"

include("mvi-core-primitives")
include("mvi-core")
include("mvi-kit-coroutine")

include("mvi-sideeffect-solo")

include("mvi-test-util")
include("mvi-test-common")