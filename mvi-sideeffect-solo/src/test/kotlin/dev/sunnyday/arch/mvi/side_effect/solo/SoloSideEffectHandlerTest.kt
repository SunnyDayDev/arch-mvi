package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.coroutine.ktx.toFlow
import dev.sunnyday.arch.mvi.side_effect.solo.util.AtomicStore
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@Timeout(10, unit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class SoloSideEffectHandlerTest {

    // region Fixture

    private val dependencies = mockk<TestDependencies>()

    private val executingSideEffectsStore: AtomicStore<Array<ExecutingSideEffect<TestSideEffect>>> = spyk(
        JvmAtomicReferenceStore(
            emptyArray(),
        ),
    )

    @BeforeEach
    fun setUp() {
        SIDE_EFFECTS_STORE.set(executingSideEffectsStore)
        mockkObject(SoloSideEffectHandler)
        every { SoloSideEffectHandler.createSideEffectsStore<TestSideEffect>() } returns executingSideEffectsStore
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SoloSideEffectHandler)
        SIDE_EFFECTS_STORE.set(null)
    }

    // endregion

    // region Specification (Tests)

    // region General

    @Test
    fun `id set in rule is executing sideEffect id`() = runUnconfinedTest {
        val expectedId = ExecutingSideEffect.Id.Unique()
        val sideEffect = TestSideEffect(executionRule {
            setId(expectedId)
        })

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(sideEffect)

        val executingSideEffects = executingSideEffectsStore.get()

        val executingSideEffect = assertNotNull(executingSideEffects.single())
        assertEquals(expectedId, executingSideEffect.id)
        assertEquals(sideEffect, executingSideEffect.sideEffect)
    }

    @Test
    fun `handler tracks (updates) side effect statuses`() = runTest {
        val handler = createSoloSideEffectHandler()
        advanceUntilIdle()

        val sideEffect = TestSideEffect(executionRule {
            onEnqueue {
                delay(TEST_DELAY.milliseconds)
            }
        })

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

        sideEffect.complete()
        advanceUntilIdle()

        assertEquals(ExecutingSideEffect.ExecutionState.COMPLETED, executingSideEffect.executionState)
        assertTrue(executingSideEffectsStore.get().isEmpty())
    }

    @Test
    fun `side effect events are handler output events`() = runUnconfinedTest {
        val handler = createSoloSideEffectHandler(collectOutputEvents = false)
        val sideEffect = TestSideEffect()
        val handlerEvents = handler.outputEvents.toFlow().collectWithScope()
        val expectedEvent = Event("expected")

        handler.onSideEffect(sideEffect)

        sideEffect.send(expectedEvent)

        assertEquals(listOf(expectedEvent), handlerEvents)
    }

    // endregion

    // region Delaying execution of side effect

    @Test
    fun `delay on enqueue delays side effect execution`() = runTest {
        val delayDuration = 100.milliseconds

        val sideEffect = TestSideEffect(executionRule {
            onEnqueue {
                delay(delayDuration)
            }
        })

        val handler = createSoloSideEffectHandler()
        advanceUntilIdle()

        handler.onSideEffect(sideEffect)
        advanceTimeBy(delayDuration.inWholeMilliseconds - 1)
        runCurrent()

        assertEquals(TestSideEffect.State.ENQUEUED, sideEffect.state)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(TestSideEffect.State.EXECUTING, sideEffect.state)
    }

    @Test
    fun `await complete on enqueue delays execution until matched sideeffects complete`() = runUnconfinedTest {
        val sideEffectForAwait = TestSideEffect()
        val awaitingSideEffect = TestSideEffect(executionRule {
            onEnqueue {
                awaitComplete(InstanceFilter.Filter { it.sideEffect === sideEffectForAwait })
            }
        })

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(sideEffectForAwait)
        handler.onSideEffect(awaitingSideEffect)

        assertEquals(TestSideEffect.State.ENQUEUED, awaitingSideEffect.state)

        sideEffectForAwait.complete()

        assertEquals(TestSideEffect.State.EXECUTING, awaitingSideEffect.state)
    }

    @Test
    fun `await complete on enqueue doesn't delay execution if no matched sideeffects executing`() = runUnconfinedTest {
        val awaitingSideEffect = TestSideEffect(executionRule {
            onEnqueue {
                awaitComplete(InstanceFilter.Filter { false })
            }
        })

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(awaitingSideEffect)

        assertEquals(TestSideEffect.State.EXECUTING, awaitingSideEffect.state)
    }

    // endregion

    // region Actions to get current executing side effects

    @ParameterizedTest
    @MethodSource("provideGetExecutingSideEffectsTestCases")
    fun `getExecutingSideEffects returns current executing side effects except self`(
        testCase: SideEffectHandlerTestCase<MutableList<ExecutingSideEffect<TestSideEffect>>>,
    ) = runUnconfinedTest {
        val collector = mutableListOf<ExecutingSideEffect<TestSideEffect>>()

        val executingSideEffect = TestSideEffect(executionRule {
            setId(ExecutingSideEffect.Id.Custom("executing"))
        })
        val checkSideEffect = TestSideEffect(testCase.createSideEffectRule(collector))

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(executingSideEffect)
        handler.onSideEffect(checkSideEffect)

        testCase.onSideEffect(handler, checkSideEffect)

        assertEquals(executingSideEffect, collector.singleOrNull()?.sideEffect)
    }

    @Test
    fun `notify registered listener if matched side effect executed`() = runUnconfinedTest {
        val targetSideEffect = TestSideEffect()
        val listener = mockk<(ExecutingSideEffect<TestSideEffect>) -> Unit>(relaxed = true)
        val listenerSideEffect = TestSideEffect(executionRule {
            onExecute {
                registerListener(InstanceFilter.Filter { it.sideEffect === targetSideEffect }, listener)
            }
        })
        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(listenerSideEffect)
        handler.onSideEffect(targetSideEffect)

        verify { listener.invoke(match { it.sideEffect === targetSideEffect }) }
        confirmVerified(listener)
    }

    @Test
    fun `don't notify registered listener if unmatched side effect executed`() = runUnconfinedTest {
        val targetSideEffect = TestSideEffect()
        val listener = mockk<(ExecutingSideEffect<TestSideEffect>) -> Unit>(relaxed = true)
        val listenerSideEffect = TestSideEffect(executionRule {
            onExecute {
                registerListener(InstanceFilter.Filter { false }, listener)
            }
        })
        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(listenerSideEffect)
        handler.onSideEffect(targetSideEffect)

        confirmVerified(listener)
    }

    // endregion

    // region Skipping side effect execution

    @ParameterizedTest
    @MethodSource("provideSkipIfAlreadyExecutingTestCases")
    fun `skip if already executing specified sideeffect`(
        skipIfAlreadyExecutingRuleFactory: SideEffectHandlerTestCase<TestSideEffect>
    ) = runUnconfinedTest {
        val targetSideEffect = TestSideEffect()
        val dependendSideEffect =
            TestSideEffect(skipIfAlreadyExecutingRuleFactory.createSideEffectRule(targetSideEffect))

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(targetSideEffect)
        handler.onSideEffect(dependendSideEffect)
        targetSideEffect.complete()

        assertEquals(TestSideEffect.State.ENQUEUED, dependendSideEffect.state)
    }

    @ParameterizedTest
    @MethodSource("provideSkipIfAlreadyExecutingTestCases")
    fun `don't skip if already executing sideeffect isn't present`(
        testCase: SideEffectHandlerTestCase<TestSideEffect?>
    ) = runUnconfinedTest {
        val sideEffect = TestSideEffect(testCase.createSideEffectRule(null))

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(sideEffect)

        assertEquals(TestSideEffect.State.EXECUTING, sideEffect.state)
    }

    // endregion

    // region Side effect cancellation

    @ParameterizedTest
    @MethodSource("provideCancelOtherTestCases")
    fun `cancelOther(_) cancels other side effect`(
        testCase: SideEffectHandlerTestCase<TestSideEffect>
    ) = runUnconfinedTest {
        val targetSideEffect = TestSideEffect()
        val sideEffect = TestSideEffect(testCase.createSideEffectRule(targetSideEffect))

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(targetSideEffect)

        assertEquals(TestSideEffect.State.EXECUTING, targetSideEffect.state)

        handler.onSideEffect(sideEffect)
        testCase.onSideEffect(handler, sideEffect)

        assertEquals(TestSideEffect.State.CANCELLED, targetSideEffect.state)
    }

    @ParameterizedTest
    @MethodSource("provideSendSignalTestCases")
    fun `cancelOnSignal cancels side effect when matched signal received`(
        testCase: SideEffectHandlerTestCase<Any>,
    ) = runUnconfinedTest {
        val signal = Any()
        val signalSideEffect = TestSideEffect(testCase.createSideEffectRule(signal))
        val cancellableSideEffect = TestSideEffect(executionRule {
            onExecute {
                registerCancelOnSignal(InstanceFilter.Filter { it === signal })
            }
        })
        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(cancellableSideEffect)
        handler.onSideEffect(signalSideEffect)
        testCase.onSideEffect(handler, signalSideEffect)

        assertEquals(TestSideEffect.State.CANCELLED, cancellableSideEffect.state)
    }

    // endregion

    // endregion

    // region Utils

    private suspend fun createSoloSideEffectHandler(
        collectOutputEvents: Boolean = true,
    ): SoloSideEffectHandler<TestDependencies, TestSideEffect, Event> {
        val currentCoroutineContext = currentCoroutineContext()

        return SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>(
            dependencies = dependencies,
            coroutineScope = createTestSubScope(),
            dispatcher = requireNotNull(currentCoroutineContext[CoroutineDispatcher]),
        ).also { handler ->
            if (collectOutputEvents) {
                handler.outputEvents.toFlow().collectWithScope()
            }
        }
    }

    interface TestDependencies

    open class TestSideEffect(
        override val executionRule: SoloExecutionRule<TestSideEffect> = SoloExecutionRule.independent(),
    ) : SoloSideEffect<TestDependencies, TestSideEffect, Event> {

        var state: State = State.ENQUEUED
            private set

        private val eventChannel = Channel<Event>()

        suspend fun send(event: Event) {
            eventChannel.send(event)
        }

        fun complete() {
            eventChannel.close()
        }

        override fun execute(dependency: TestDependencies): Flow<Event> = flow {
            for (event in eventChannel) {
                emit(event)
            }
        }
            .onStart { state = State.EXECUTING }
            .onCompletion { state = if (it != null) State.CANCELLED else State.COMPLETED }

        enum class State {
            ENQUEUED,
            EXECUTING,
            COMPLETED,
            CANCELLED,
        }
    }

    abstract class SideEffectHandlerTestCase<T>(
        private val name: String,
    ) {

        abstract fun createSideEffectRule(target: T): SoloExecutionRule<TestSideEffect>

        open fun onSideEffect(
            handler: SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>,
            ruleSideEffect: TestSideEffect,
        ) = Unit

        override fun toString(): String {
            return name
        }

        companion object {

            fun <T> create(
                name: String,
                onSideEffect: (
                    handler: SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>,
                    ruleSideEffect: TestSideEffect,
                ) -> Unit = { _, _ -> },
                executionRuleConfig: SoloExecutionRuleConfig<TestSideEffect>.(target: T) -> Unit,
            ): SideEffectHandlerTestCase<T> = object : SideEffectHandlerTestCase<T>(name) {

                override fun createSideEffectRule(target: T): SoloExecutionRule<TestSideEffect> {
                    return executionRule { executionRuleConfig(target) }
                }

                override fun onSideEffect(
                    handler: SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>,
                    ruleSideEffect: TestSideEffect
                ) {
                    onSideEffect.invoke(handler, ruleSideEffect)
                }
            }
        }
    }

    private companion object {

        const val TEST_DELAY = 100L

        private val SIDE_EFFECTS_STORE = ThreadLocal<AtomicStore<Array<ExecutingSideEffect<TestSideEffect>>>>()

        @JvmStatic
        fun provideCancelOtherTestCases(): List<SideEffectHandlerTestCase<TestSideEffect>> {
            return listOf(
                SideEffectHandlerTestCase.create("onEnqueue") { target ->
                    onEnqueue {
                        cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                    }
                },
                SideEffectHandlerTestCase.create("onExecute") { target ->
                    onExecute {
                        cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                    }
                },
                SideEffectHandlerTestCase.create("onCancel", cancelRuleSideEffect()) { target ->
                    onCancel {
                        cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                    }
                }
            )
        }

        @JvmStatic
        fun provideSkipIfAlreadyExecutingTestCases(): List<SideEffectHandlerTestCase<TestSideEffect?>> {
            return listOf(
                SideEffectHandlerTestCase.create("onEnqueue") { target ->
                    onEnqueue {
                        skipIfAlreadyExecuting(InstanceFilter.Filter { it.sideEffect === target })
                    }
                },
                SideEffectHandlerTestCase.create("onExecute") { target ->
                    onExecute {
                        skipIfAlreadyExecuting(InstanceFilter.Filter { it.sideEffect === target })
                    }
                },
            )
        }

        @JvmStatic
        fun provideSendSignalTestCases(): List<SideEffectHandlerTestCase<Any>> {
            return listOf(
                SideEffectHandlerTestCase.create("onEnqueue") { signal ->
                    onEnqueue {
                        sendSignal(signal)
                    }
                },
                SideEffectHandlerTestCase.create("onExecute") { signal ->
                    onExecute {
                        sendSignal(signal)
                    }
                },
                SideEffectHandlerTestCase.create("onCancel", cancelRuleSideEffect()) { signal ->
                    onCancel {
                        sendSignal(signal)
                    }
                },
            )
        }

        @JvmStatic
        fun provideGetExecutingSideEffectsTestCases(): List<SideEffectHandlerTestCase<MutableList<ExecutingSideEffect<TestSideEffect>>>> {
            return listOf(
                SideEffectHandlerTestCase.create("onEnqueue") { collector ->
                    onEnqueue {
                        collector.addAll(getExecutingSideEffects())
                    }
                },
                SideEffectHandlerTestCase.create("onExecute") { collector ->
                    onExecute {
                        collector.addAll(getExecutingSideEffects())
                    }
                },
                SideEffectHandlerTestCase.create("onCancel", cancelRuleSideEffect()) { collector ->
                    onCancel {
                        collector.addAll(getExecutingSideEffects())
                    }
                },
            )
        }

        private fun cancelRuleSideEffect(): (
            handler: SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>,
            ruleSideEffect: TestSideEffect,
        ) -> Unit {
            return { _, ruleSideEffect ->
                SIDE_EFFECTS_STORE.get().get()
                    .firstOrNull { it.sideEffect === ruleSideEffect }
                    ?.cancel()
            }
        }
    }
}