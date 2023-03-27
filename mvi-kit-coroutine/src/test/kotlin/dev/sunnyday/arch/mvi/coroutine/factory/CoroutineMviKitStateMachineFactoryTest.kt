package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.StateTransition
import dev.sunnyday.arch.mvi.StateTransitionListener
import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import dev.sunnyday.arch.mvi.coroutine.CoroutineStateMachine
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.test.*
import dev.sunnyday.arch.mvi.test.common.createTestStateMachine
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class CoroutineMviKitStateMachineFactoryTest {

    @Test
    fun `create coroutine state machine`() {
        mockkObject(CoroutineScopes)
        mockkConstructor(CoroutineStateMachine::class)

        val parentCoroutine = CoroutineScope(EmptyCoroutineContext)
        val stateMachineCoroutine = CoroutineScope(EmptyCoroutineContext)
        val factoryCallContext = MviFactoryCallContext.create(
            CoroutineFactoryContext(parentCoroutine),
        )

        val initialState = State("initial")
        val reducer = stub<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = stub<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()

        every { CoroutineScopes.MviCoroutineScope(any()) } returns stateMachineCoroutine

        val stateMachineConstructorRule = ConstructorRule.create<CoroutineStateMachine<State, Event, SideEffect>>(
            EqMatcher(initialState, ref = true),
            EqMatcher(stateMachineCoroutine, ref = true),
            EqMatcher(reducer, ref = true),
            EqMatcher(stateTransitionListener, ref = true),
        )

        val stateMachine = factoryCallContext.runWithFactoryContext {
            CoroutineMviKitStateMachineFactory().createStateMachine(
                initialState = initialState,
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )
        }

        stateMachineConstructorRule.verifyConstructorCalled(stateMachine)
        verify { CoroutineScopes.MviCoroutineScope(parentCoroutine) }

        unmockkAll()
    }
}