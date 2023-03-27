package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.coroutine.CoroutineMviFeature
import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import dev.sunnyday.arch.mvi.coroutine.factory.instance.CoroutineStateMachineInstanceFactoryScope
import dev.sunnyday.arch.mvi.factory.MviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import kotlinx.coroutines.CoroutineScope

class CoroutineMviKitFeatureFactory : MviKitFeatureFactory {

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        initialState: State,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {
        val coroutineScope = getCoroutineScope()

        val stateMachine = createStateMachine(
            initialState = initialState,
            stateMachineFactory = stateMachineFactory,
        )

        return CoroutineMviFeature(
            coroutineScope = coroutineScope,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachine = stateMachine,
            onReadyCallback = onReadyCallback,
        )
    }

    private fun getCoroutineScope(): CoroutineScope {
        return CoroutineScopes.MviCoroutineScope(
            parent = CoroutineFactoryContext.getParentCoroutineScope(),
        )
    }

    private fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        initialState: State,
        stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
    ): StateMachine<State, Event, SideEffect> {
        val stateMachineFactoryScope: StateMachineInstanceFactory.FactoryScope<State, Event, SideEffect> =
            CoroutineStateMachineInstanceFactoryScope(
                initialState = initialState,
            )

        return stateMachineFactory.run { stateMachineFactoryScope.createStateMachine() }
    }
}