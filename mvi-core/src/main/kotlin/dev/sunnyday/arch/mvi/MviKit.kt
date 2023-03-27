package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstance

object MviKit : ContextEnabledMviKitInstance() {

    private var _stateMachineFactory: MviKitStateMachineFactory? = null
    private var _featureFactory: MviKitFeatureFactory? = null
    private var _starterFactory: MviKitFeatureStarterFactory? = null

    override val stateMachineFactory: MviKitStateMachineFactory
        get() = requireNotNull(_stateMachineFactory, ::requireInitializationMessage)
    override val featureFactory: MviKitFeatureFactory
        get() = requireNotNull(_featureFactory, ::requireInitializationMessage)
    override val starterFactory: MviKitFeatureStarterFactory
        get() = requireNotNull(_starterFactory, ::requireInitializationMessage)

    var isReady: Boolean = false
        private set

    fun setup(
        stateMachineFactory: MviKitStateMachineFactory,
        featureFactory: MviKitFeatureFactory,
        starterFactory: MviKitFeatureStarterFactory,
    ) {
        _stateMachineFactory = stateMachineFactory
        _featureFactory = featureFactory
        _starterFactory = starterFactory

        isReady = true
    }

    fun setup(instance: ContextEnabledMviKitInstance) {
        _stateMachineFactory = instance
        _featureFactory = instance
        _starterFactory = instance

        isReady = true
    }

    fun reset() {
        isReady = false

        _stateMachineFactory = null
        _featureFactory = null
        _starterFactory = null
    }

    private fun requireInitializationMessage(): String {
        return "Before you can use MviKit it must be configured with MviKit.setup(...)"
    }
}