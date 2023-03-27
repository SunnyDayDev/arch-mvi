package dev.sunnyday.arch.mvi.coroutine.ktx

import dev.sunnyday.arch.mvi.test.collectWithScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsKtTest {

    @Test
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `take flow until signal`() = runTest {
        val flow: Flow<Int> = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        val signal = MutableSharedFlow<Unit>()

        val items: List<Int> = flow
            .map { delay(100); it }
            .takeUntil(signal)
            .collectWithScope()

        launch {
            delay(200)
            signal.emit(Unit)
        }

        advanceTimeBy(500)

        assertEquals(listOf(1, 2), items)
    }
}