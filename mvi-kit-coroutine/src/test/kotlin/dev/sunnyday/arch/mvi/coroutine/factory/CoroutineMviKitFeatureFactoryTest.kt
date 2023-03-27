package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.coroutine.CoroutineMviFeature
import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import dev.sunnyday.arch.mvi.coroutine.factory.instance.CoroutineStateMachineInstanceFactoryScope
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class CoroutineMviKitFeatureFactoryTest {

    @Test
    fun `create coroutine MviFeature`() {
        val parentCoroutineScope = CoroutineScope(EmptyCoroutineContext)
        val featureCoroutineScope = CoroutineScopes.MviCoroutineScope()

        mockkObject(CoroutineScopes)
        mockkConstructor(CoroutineStateMachineInstanceFactoryScope::class, CoroutineMviFeature::class)

        val initialState = State("initial")
        val eventHandler = stub<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = stub<SideEffectHandler<SideEffect, Event>>()
        val stateMachineFactoryMock = mockk<StateMachineInstanceFactory<State, Event, SideEffect>>()
        val stateMachineFactory = stub(stateMachineFactoryMock)
        val onReadyCallback = stub<OnReadyCallback>()
        val stateMachine = stub<StateMachine<State, Event, SideEffect>>()

        every {
            stateMachineFactory.run {
                any<StateMachineInstanceFactory.FactoryScope<State, Event, SideEffect>>()
                    .createStateMachine()
            }
        } returns stateMachine

        every { CoroutineScopes.MviCoroutineScope(any()) } returns featureCoroutineScope

        val featureConstructorRule = ConstructorRule.create<CoroutineMviFeature<State, InputEvent, Event, SideEffect>>(
            EqMatcher(featureCoroutineScope, ref = true),
            EqMatcher(eventHandler, ref = true),
            EqMatcher(sideEffectHandler, ref = true),
            EqMatcher(stateMachine, ref = true),
            EqMatcher(onReadyCallback, ref = true),
        )

        val coroutineFactoryContext = CoroutineFactoryContext().apply {
            this.parentCoroutineScope = parentCoroutineScope
        }
        val feature = MviFactoryCallContext.create(coroutineFactoryContext).runWithFactoryContext {
            CoroutineMviKitFeatureFactory()
                .createFeature(
                    initialState = initialState,
                    eventHandler = eventHandler,
                    sideEffectHandler = sideEffectHandler,
                    stateMachineFactory = stateMachineFactory,
                    onReadyCallback = onReadyCallback,
                )
        }

        featureConstructorRule.verifyConstructorCalled(feature)
        verify { CoroutineScopes.MviCoroutineScope(refEq(parentCoroutineScope)) }

        unmockkAll()
    }
}