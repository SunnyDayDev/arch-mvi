package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `destruction of Update`() {
        val (state, sideEffects) = Update.stateWithSideEffects(State(), SideEffect())

        assertEquals(State(), state)
        assertEquals(listOf(SideEffect()), sideEffects)
    }

    @Test
    fun `Update_toString() describes content`() {
        assertEquals(
            "Update(state: State(name=state), sideEffects: SideEffect(name=sideEffect))",
            Update.stateWithSideEffects(State(), SideEffect()).toString(),
        )

        assertEquals("Update(nothing)", Update.nothing().toString())
    }
}