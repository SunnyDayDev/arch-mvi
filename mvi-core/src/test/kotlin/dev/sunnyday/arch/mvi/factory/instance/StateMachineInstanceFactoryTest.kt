package dev.sunnyday.arch.mvi.factory.instance

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.*
import org.junit.jupiter.api.Test

class StateMachineInstanceFactoryTest {

    @Test
    fun `create state machine`() {
        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()
        val factoryScope = mockk<StateMachineInstanceFactory.FactoryScope<State, Event, SideEffect>>(relaxed = true)

        StateMachineInstanceFactory {
            createStateMachine(
                reducer = reducer,
            )
        }.run { factoryScope.createStateMachine() }

        StateMachineInstanceFactory {
            createStateMachine(
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )
        }.run { factoryScope.createStateMachine() }

        verifyOrder {
            factoryScope.createStateMachine(
                reducer = refEq(reducer),
                stateTransitionListener = isNull(),
            )

            factoryScope.createStateMachine(
                reducer = refEq(reducer),
                stateTransitionListener = refEq(stateTransitionListener),
            )
        }
    }
}