package dev.sunnyday.arch.mvi.kit

import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.MviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviKitFeatureStarterFactory
import dev.sunnyday.arch.mvi.factory.MviKitStateMachineFactory

interface MviKitInstance :
    MviKitStateMachineFactory,
    MviKitFeatureFactory,
    MviKitFeatureStarterFactory,
    MviFactoryCallContext.Element {

    override val key: MviFactoryCallContext.Key<*>
        get() = Key

    companion object Key : MviFactoryCallContext.Key<MviKitInstance>
}
