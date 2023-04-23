package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.coroutine.ktx.toFlow
import dev.sunnyday.arch.mvi.coroutine.ktx.toObservable
import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.primitive.SharedObservableEvent
import dev.sunnyday.arch.mvi.side_effect.solo.util.JvmAtomicReferenceStore
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@Timeout(10, unit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class SoloSideEffectHandlerTest {

    private val dependencies = mockk<TestDependencies>()

    private val executingSideEffectsStore = spyk(
        JvmAtomicReferenceStore<Array<ExecutingSideEffect<TestSideEffect>>>(
            emptyArray(),
        ),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(SoloSideEffectHandler)
        every { SoloSideEffectHandler.createSideEffectsStore<TestSideEffect>() } returns executingSideEffectsStore
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SoloSideEffectHandler)
    }

    @Test
    fun `handler properly tracks side effect statuses`() = runTest {
        val handler = createSoloSideEffectHandler()
        advanceUntilIdle()

        val sideEffect: TestSideEffect = object : TestSideEffect {
            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue {
                    delay(TEST_DELAY.milliseconds)
                }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = flow {
                delay(TEST_DELAY.milliseconds)
            }
        }

        handler.onSideEffect(sideEffect)
        runCurrent()

        val executingSideEffect = executingSideEffectsStore.get().singleOrNull()
        assertNotNull(executingSideEffect)
        assertEquals(ExecutingSideEffect.ExecutionState.ENQUEUED, executingSideEffect.executionState)

        advanceTimeBy(TEST_DELAY - 1)

        assertEquals(ExecutingSideEffect.ExecutionState.ENQUEUED, executingSideEffect.executionState)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(ExecutingSideEffect.ExecutionState.EXECUTING, executingSideEffect.executionState)

        advanceUntilIdle()

        assertEquals(ExecutingSideEffect.ExecutionState.COMPLETED, executingSideEffect.executionState)
        assertTrue(executingSideEffectsStore.get().isEmpty())
    }

    @Test
    fun `rule id is sideEffect id`() = runUnconfinedTest {
        val expectedId = ExecutingSideEffect.Id.Custom("custom")
        val sideEffect: TestSideEffect = object : TestSideEffect {
            override val executionRule = executionRule<TestSideEffect> {
                setId(expectedId)
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = MutableSharedFlow()
        }

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(sideEffect)

        val executingSideEffects = executingSideEffectsStore.get()

        assertEquals(1, executingSideEffects.size)
        assertEquals(expectedId, executingSideEffects.single().id)
        assertEquals(sideEffect, executingSideEffects.single().sideEffect)
    }

    @Test
    fun `delay on enqueue delays side effect execution`() = runTest {
        val delayDuration = 100.milliseconds
        val flow = spyk(emptyFlow<Event>())

        val sideEffect = object : TestSideEffect {
            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue {
                    delay(delayDuration)
                }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = flow
        }

        val handler = createSoloSideEffectHandler()
        advanceUntilIdle()

        handler.onSideEffect(sideEffect)
        advanceTimeBy(delayDuration.inWholeMilliseconds - 1)
        runCurrent()

        confirmVerified(flow)

        advanceTimeBy(1)
        runCurrent()
        coVerify { flow.collect(any()) }
    }

    @Test
    fun `await complete on enqueue delays execution until specified sideeffects complete`() = runUnconfinedTest {
        val root = object : TestSideEffect {

            private val completeChannel = Channel<Unit>()

            override fun execute(dependency: TestDependencies): Flow<Event> = flow { completeChannel.receive() }

            suspend fun complete() = completeChannel.send(Unit)
        }

        val dependentFlow = spyk(emptyFlow<Event>())
        val dependent = object : TestSideEffect {

            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue {
                    awaitComplete(InstanceFilter.Filter { it.sideEffect === root })
                }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = dependentFlow
        }

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(root)
        handler.onSideEffect(dependent)

        confirmVerified(dependentFlow)

        root.complete()

        coVerify { dependentFlow.collect(any()) }
    }

    private suspend fun TestScope.createSoloSideEffectHandler(): SoloSideEffectHandler<TestDependencies, TestSideEffect, Event> {
        val currentCoroutineContext = currentCoroutineContext()

        return SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>(
            dependencies = dependencies,
            coroutineScope = this,
            dispatcher = requireNotNull(currentCoroutineContext[CoroutineDispatcher]),
        ).also { it.outputEvents.toFlow().collectWithScope() }
    }

    interface TestDependencies

    interface TestSideEffect : SoloSideEffect<TestDependencies, TestSideEffect, Event> {

        override fun execute(dependency: TestDependencies): Flow<Event> = emptyFlow()
    }

    private companion object {

        const val TEST_DELAY = 100L
    }
}