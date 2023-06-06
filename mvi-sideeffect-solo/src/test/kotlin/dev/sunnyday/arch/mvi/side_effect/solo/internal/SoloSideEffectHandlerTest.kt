package dev.sunnyday.arch.mvi.side_effect.solo.internal

import dev.sunnyday.arch.mvi.coroutine.ktx.toFlow
import dev.sunnyday.arch.mvi.side_effect.solo.*
import dev.sunnyday.arch.mvi.side_effect.solo.filter.side_effect.sideEffectsWithType
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
import kotlin.test.*
import kotlin.time.Duration
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

    // region Dispatchers (execution threads)

    @Test
    fun `side effect executes on specified common dispatcher`() = runUnconfinedTest {
        val expectedDispatcher = UnconfinedTestDispatcher()
        val handler = createSoloSideEffectHandler(sideEffectDispatcher = expectedDispatcher)

        val sideEffect = DispatcherTrackingTestSideEffect()

        handler.onSideEffect(sideEffect)

        assertSame(expectedDispatcher, sideEffect.executeDispatcher)
    }

    @Test
    fun `side effect executes on specified side effect dispatcher`() = runUnconfinedTest {
        val expectedDispatcher = UnconfinedTestDispatcher()
        val handler = createSoloSideEffectHandler()

        val sideEffect = DispatcherTrackingTestSideEffect(executionRule {
            setDispatcher(expectedDispatcher)
        })

        handler.onSideEffect(sideEffect)

        assertSame(expectedDispatcher, sideEffect.executeDispatcher)
    }

    // endregion

    // region Skipping side effect execution

    @ParameterizedTest
    @MethodSource("provideSkipIfAlreadyExecutingTestCases")
    fun `skip if already executing specified sideeffect`(
        skipIfAlreadyExecutingRuleFactory: SideEffectHandlerTestCase<TestSideEffect>
    ) = runUnconfinedTest {
        val targetSideEffect = TestSideEffect()
        val dependendSideEffect = TestSideEffect(
            skipIfAlreadyExecutingRuleFactory.createSideEffectRule(targetSideEffect)
        )

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(targetSideEffect)
        handler.onSideEffect(dependendSideEffect)
        targetSideEffect.complete()

        assertEquals(TestSideEffect.State.UNEXECUTED, dependendSideEffect.state)
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

    @ParameterizedTest
    @MethodSource("provideSkipIfAlreadyExecutingTestCases")
    fun `don't skip if already executing is self`() = runUnconfinedTest {
        val sideEffect = TestSideEffect(executionRule {
            onEnqueue {
                skipIfAlreadyExecuting(sideEffectsWithType<TestSideEffect>())
            }
        })

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(sideEffect)

        assertEquals(TestSideEffect.State.EXECUTING, sideEffect.state)
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

        assertEquals(TestSideEffect.State.UNEXECUTED, sideEffect.state)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(TestSideEffect.State.EXECUTING, sideEffect.state)
    }

    @Test
    fun `don't delay on enqueue if side effect is skipped`() = runTest {
        val sideEffect = TestSideEffect(executionRule {
            onEnqueue {
                skipIfAlreadyExecuting(InstanceFilter.Filter { true })
                delay(100.milliseconds)
            }
        })

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(TestSideEffect())
        advanceUntilIdle()

        val expectedTime = testScheduler.currentTime

        handler.onSideEffect(sideEffect)
        advanceUntilIdle()

        assertEquals(expectedTime, testScheduler.currentTime)
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

        assertEquals(TestSideEffect.State.UNEXECUTED, awaitingSideEffect.state)

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

    @Test
    fun `don't await complete on enqueue if side effect is skipped`() = runTest {
        val sideEffect = TestSideEffect(executionRule {
            onEnqueue {
                skipIfAlreadyExecuting(InstanceFilter.Filter { true })
                awaitComplete(InstanceFilter.Filter { true })
            }
        })
        val awaitCompleteTriggerSideEffect = TestSideEffect()

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(TestSideEffect())
        runCurrent()

        handler.onSideEffect(sideEffect)
        awaitCompleteTriggerSideEffect.complete()
        runCurrent()

        assertEquals(TestSideEffect.State.UNEXECUTED, sideEffect.state)
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

    // region Miscellaneous

    @Test
    fun `side effects atomic store is JvmAtomicReferenceStore`() {
        unmockkObject(SoloSideEffectHandler)

        val store = SoloSideEffectHandler.createSideEffectsStore<TestSideEffect>()

        assertIs<JvmAtomicReferenceStore<*>>(store)
    }

    // endregion

    // endregion

    // region Utils

    private suspend fun TestScope.createSoloSideEffectHandler(
        collectOutputEvents: Boolean = true,
        sideEffectDispatcher: CoroutineDispatcher? = null,
    ): SoloSideEffectHandler<TestDependencies, TestSideEffect, Event> {
        val currentCoroutineContext = currentCoroutineContext()

        return SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>(
            dependencies = dependencies,
            coroutineScope = createTestSubScope(),
            sideEffectDispatcher = sideEffectDispatcher ?: requireNotNull(currentCoroutineContext[CoroutineDispatcher]),
        ).also { handler ->
            if (collectOutputEvents) {
                handler.outputEvents.toFlow().collectWithScope()
                runCurrent()
            }
        }
    }

    interface TestDependencies

    open class TestSideEffect(
        override val executionRule: SoloExecutionRule<TestSideEffect> = SoloExecutionRule.independent(),
        private val minDuration: Duration = 0.milliseconds,
        private val tag: String? = null,
    ) : SoloSideEffect<TestDependencies, TestSideEffect, Event> {

        var state: State = State.UNEXECUTED
            private set

        private val eventChannel = Channel<Event>()

        suspend fun send(event: Event) {
            eventChannel.send(event)
        }

        fun complete() {
            eventChannel.close()
        }

        override fun execute(dependency: TestDependencies): Flow<Event> = merge(eventsFlow(), minDurationFlow())
            .onStart { state = State.EXECUTING }
            .onCompletion { state = if (it != null) State.CANCELLED else State.COMPLETED }

        private fun eventsFlow(): Flow<Event> {
            return flow {
                for (event in eventChannel) {
                    emit(event)
                }
            }
        }

        private fun minDurationFlow(): Flow<Event> {
            return if (minDuration.isPositive()) {
                flow { delay(minDuration.inWholeMilliseconds) }
            } else {
                emptyFlow()
            }
        }

        override fun toString(): String {
            return tag?.let { "TestSideEffect($it)" } ?: super.toString()
        }

        enum class State {
            UNEXECUTED,
            EXECUTING,
            COMPLETED,
            CANCELLED,
        }
    }

    class DispatcherTrackingTestSideEffect(
        executionRule: SoloExecutionRule<TestSideEffect> = SoloExecutionRule.independent(),
    ) : TestSideEffect(executionRule) {

        var executeDispatcher: CoroutineDispatcher? = null

        override fun execute(dependency: TestDependencies): Flow<Event> {
            return super.execute(dependency)
                .onStart { executeDispatcher = currentCoroutineContext()[CoroutineDispatcher] }
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
                        getExecutingSideEffects(collector::addAll)
                    }
                },
                SideEffectHandlerTestCase.create("onExecute") { collector ->
                    onExecute {
                        getExecutingSideEffects(collector::addAll)
                    }
                },
                SideEffectHandlerTestCase.create("onCancel", cancelRuleSideEffect()) { collector ->
                    onCancel {
                        getExecutingSideEffects(collector::addAll)
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