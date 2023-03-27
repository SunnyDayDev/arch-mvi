package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MviKitStateMachineFactoryTest {

    @Test
    fun `state machine factory default args`() {
        val factory = mockk<MviKitStateMachineFactory>(relaxed = true)

        val initialState = mockk<State>()
        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()

        factory.createStateMachine(
            initialState = initialState,
            reducer = reducer,
        )

        verify {
            factory.createStateMachine(
                initialState = refEq(initialState),
                reducer = refEq(reducer),
                stateTransitionListener = isNull(),
            )
        }
    }

    @Test
    fun `provide factory call key`() {
        val factory: MviKitStateMachineFactory = TestMviKitStateMachineFactory()
        assertEquals(MviKitStateMachineFactory, factory.key)
    }

    @Test
    fun `get element by key`() {
        val factory: MviKitStateMachineFactory = TestMviKitStateMachineFactory()
        assertSame(factory, factory[MviKitStateMachineFactory])
    }

    private class TestMviKitStateMachineFactory : MviKitStateMachineFactory {
        override fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
            initialState: State,
            reducer: Reducer<State, Event, Update<State, SideEffect>>,
            stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
        ): StateMachine<State, Event, SideEffect> {
            TODO("Not yet implemented")
        }

    }
}