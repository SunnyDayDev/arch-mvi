package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UpdateTest {

    @Test
    fun `all only state updates are equals`() {
        val updates = listOf(
            Update.state(State()),
            Update.stateWithSideEffects(State(), emptyList()),
            Update.stateWithSideEffects(State()),
        )

        assertTrue(updates.distinct().size == 1)
        assertTrue(updates.map { it.hashCode() }.distinct().size == 1)
    }

    @Test
    fun `all only side effect updates are equals`() {
        val updates = listOf(
            Update.sideEffects(listOf(SideEffect())),
            Update.sideEffects(SideEffect()),
        )

        assertTrue(updates.distinct().size == 1)
        assertTrue(updates.map { it.hashCode() }.distinct().size == 1)
    }

    @Test
    fun `all nothing updates are equals`() {
        val updates = listOf(
            Update.nothing(),
            Update.sideEffects(listOf()),
            Update.sideEffects(),
        )

        assertTrue(updates.distinct().size == 1)
        assertTrue(updates.map { it.hashCode() }.distinct().size == 1)
    }

    @Test
    fun `all full updates are equals`() {
        val updates = listOf(
            Update.stateWithSideEffects(State(), SideEffect()),
            Update.stateWithSideEffects(State(), listOf(SideEffect())),
        )

        assertTrue(updates.distinct().size == 1)
        assertTrue(updates.map { it.hashCode() }.distinct().size == 1)
    }
}