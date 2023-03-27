[![statusImage](https://ci.sunnyday.dev/app/rest/builds/buildType:ArchMvi_Test,branch:name:main/statusIcon)](https://ci.sunnyday.dev/buildConfiguration/ArchMvi_Test/lastFinished?branch=%3Cdefault%3E)
[![coverageImage](https://img.shields.io/endpoint?url=https://kvdb.io/PY9VzGdCHe8YPbKvepE4y4/arch-mvi.main.coverage&logo=TeamCity)](https://ci.sunnyday.dev/buildConfiguration/ArchMvi_Test/lastFinished?buildTab=tests&branch=%3Cdefault%3E)
[![statusImage](https://img.shields.io/badge/status-pre--alpha-orange)](https://github.com/users/SunnyDayDev/projects/2/views/3)
[![jitpackImage](https://jitpack.io/v/dev.sunnyday/arch-mvi.svg)](https://jitpack.io/#dev.sunnyday/arch-mvi)

# arch-mvi
Kotlin MVI framework

# Integration
```kotlin
// build.gradle.kts

implementation("dev.sunnyday.arch-mvi:mvi-core:$version")
implementation("dev.sunnyday.arch-mvi:mvi-kit-coroutine:$version")
```

```kotlin
// Somewhere in the code

import dev.sunnyday.arch.mvi.coroutine.setupFactories
import dev.sunnyday.arch.mvi.MviKit

fun initMvi() {
  MviKit.setupFactories()
}
```

# Status
In progress. README will be updated later.
