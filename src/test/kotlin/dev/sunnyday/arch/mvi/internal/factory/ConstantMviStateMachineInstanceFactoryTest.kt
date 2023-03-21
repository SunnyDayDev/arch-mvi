package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.factory.MviStateMachineInstanceFactory
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class ConstantMviStateMachineInstanceFactoryTest {

    @Test
    fun `just provide containing state machine on any call`() {
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>()
        val stateMachineFactoryScope = mockk<MviStateMachineInstanceFactory.FactoryScope<State, Event, SideEffect>>()

        val actualStateMachine = ConstantMviStateMachineInstanceFactory(stateMachine).run {
            stateMachineFactoryScope.createStateMachine()
        }

        assertSame(stateMachine, actualStateMachine)
    }
}