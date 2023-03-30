package dev.sunnyday.arch.mvi.side_effect.particle

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class ExecutionSideEffectTest {

    @Test
    fun `ids are unique`() {
        assertNotEquals(ExecutingSideEffect.Id.Unique(), ExecutingSideEffect.Id.Unique())
        assertFalse(ExecutingSideEffect.Id.Undefined.equals(ExecutingSideEffect.Id.Unique()))
        assertFalse(ExecutingSideEffect.Id.Custom(777).equals(ExecutingSideEffect.Id.Unique()))
    }

    @Test
    fun `custom ids compares by value`() {
        assertEquals(ExecutingSideEffect.Id.Custom(777), ExecutingSideEffect.Id.Custom(777))

        assertNotEquals(ExecutingSideEffect.Id.Custom(777), ExecutingSideEffect.Id.Custom(999))
        assertNotEquals(ExecutingSideEffect.Id.Custom(777), ExecutingSideEffect.Id.Custom(777L))
        assertNotEquals(ExecutingSideEffect.Id.Custom(777L), ExecutingSideEffect.Id.Custom(777))
        assertNotEquals(ExecutingSideEffect.Id.Custom(777), ExecutingSideEffect.Id.Custom(777.0))
        assertNotEquals(ExecutingSideEffect.Id.Custom(777), ExecutingSideEffect.Id.Custom("777"))
    }
}