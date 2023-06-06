package dev.sunnyday.arch.mvi.side_effect.solo

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExecutingSideEffectTest {

    @Test
    fun `UniqueId toString() prints short name`() {
        println(ExecutingSideEffect.Id.Unique().toString())
        assertTrue(
            ExecutingSideEffect.Id.Unique().toString()
                .startsWith("Id.Unique@")
        )
    }
}