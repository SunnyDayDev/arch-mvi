package dev.sunnyday.arch.mvi.coroutine

import dev.sunnyday.arch.mvi.MviKit
import dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstance
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitFeatureFactory
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitFeatureStarterFactory
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitStateMachineFactory
import dev.sunnyday.arch.mvi.coroutine.kit.ExtendedCoroutineMviKitInstance
import dev.sunnyday.arch.mvi.kit.MviKitInstance
import kotlinx.coroutines.CoroutineScope

fun MviKit.setupFactories() {
    setup(
        stateMachineFactory = CoroutineMviKitStateMachineFactory(),
        featureFactory = CoroutineMviKitFeatureFactory(),
        starterFactory = CoroutineMviKitFeatureStarterFactory(),
    )
}

fun MviKitInstance.withParentCoroutine(coroutineScope: CoroutineScope): ContextEnabledMviKitInstance {
    return extendWithCoroutineKit()
        .apply { parentCoroutineScope = coroutineScope }
}

private fun MviKitInstance.extendWithCoroutineKit(): ExtendedCoroutineMviKitInstance {
    return if (this is ExtendedCoroutineMviKitInstance) {
        this
    } else {
        ExtendedCoroutineMviKitInstance(this)
    }
}