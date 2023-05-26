package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.coroutine.ktx.toFlow
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
    fun `rule id is sideEffect id`() = runUnconfinedTest {
        val expectedId = ExecutingSideEffect.Id.Custom("custom")
        val sideEffect = TestSideEffect(executionRule {
            setId(expectedId)
        })

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

    @ParameterizedTest
    @MethodSource("provideGetExecutingSideEffectsRules")
    fun `getExecutingSideEffects returns current executing side effects`(
        testCase: SideEffectTestCaseStrategy<MutableList<ExecutingSideEffect<TestSideEffect>>>,
    ) = runUnconfinedTest {
        val collector = mutableListOf<ExecutingSideEffect<TestSideEffect>>()
        val executingSideEffect = TestSideEffect(executionRule {
            setId(ExecutingSideEffect.Id.Custom("target"))
        })
        val checkSideEffect = TestSideEffect(testCase.createSideEffectRule(collector))

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(executingSideEffect)
        handler.onSideEffect(checkSideEffect)

        testCase.onSideEffect(handler, checkSideEffect)

        assertEquals(executingSideEffect, collector.singleOrNull()?.sideEffect)
    }

    @Test
    fun `await complete on enqueue delays execution until specified sideeffects complete`() = runUnconfinedTest {
        val root = TestSideEffect()

        val dependent = TestSideEffect(executionRule {
            onEnqueue {
                awaitComplete(InstanceFilter.Filter { it.sideEffect === root })
            }
        })

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(root)
        handler.onSideEffect(dependent)

        assertEquals(TestSideEffect.State.UNEXECUTED, dependent.state)

        root.complete()

        assertEquals(TestSideEffect.State.EXECUTING, dependent.state)
    }

    @ParameterizedTest
    @MethodSource("provideSkipIfAlreadyExecutingRules")
    fun `skip if already executing specified sideeffect`(
        skipIfAlreadyExecutingRuleFactory: SideEffectTestCaseStrategy<TestSideEffect>
    ) = runUnconfinedTest {
        val targetSideEffect = TestSideEffect()
        val dependendSideEffect =
            TestSideEffect(skipIfAlreadyExecutingRuleFactory.createSideEffectRule(targetSideEffect))

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(targetSideEffect)
        handler.onSideEffect(dependendSideEffect)
        targetSideEffect.complete()

        assertEquals(TestSideEffect.State.UNEXECUTED, dependendSideEffect.state)
    }

    @ParameterizedTest
    @MethodSource("provideSkipIfAlreadyExecutingRules")
    fun `don't skip if already executing sideeffect isn't present`(
        testCase: SideEffectTestCaseStrategy<TestSideEffect?>
    ) = runUnconfinedTest {
        val sideEffect = TestSideEffect(testCase.createSideEffectRule(null))

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(sideEffect)

        assertEquals(TestSideEffect.State.EXECUTING, sideEffect.state)
    }

    @ParameterizedTest
    @MethodSource("provideCancelOtherRules")
    fun `cancelOther(_) cancels other side effect`(
        testCase: SideEffectTestCaseStrategy<TestSideEffect>
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
    @MethodSource("provideSendSignalRules")
    fun `cancelOnSignal cancels side effect when signal received`(
        testCase: SideEffectTestCaseStrategy<Any>,
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

    @Test
    fun `notify registered listener for specified side effect executed`() = runUnconfinedTest {
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
    fun `don't notify registered listener if unspecified side effect executed`() = runUnconfinedTest {
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

    @Test
    fun `side effect events is handler output events`() = runUnconfinedTest {
        val handler = createSoloSideEffectHandler(collectOutputEvents = false)
        val sideEffect = TestSideEffect()
        val handlerEvents = handler.outputEvents.toFlow().collectWithScope()
        val expectedEvent = Event("expected")

        handler.onSideEffect(sideEffect)

        sideEffect.send(expectedEvent)

        assertEquals(listOf(expectedEvent), handlerEvents)
    }

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

        var state: State = State.UNEXECUTED
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
            UNEXECUTED,
            EXECUTING,
            COMPLETED,
            CANCELLED,
        }
    }

    abstract class SideEffectTestCaseStrategy<T>(
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
                ruleFactory: (target: T) -> SoloExecutionRule<TestSideEffect>,
            ): SideEffectTestCaseStrategy<T> = object : SideEffectTestCaseStrategy<T>(name) {

                override fun createSideEffectRule(target: T): SoloExecutionRule<TestSideEffect> {
                    return ruleFactory.invoke(target)
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

        private val CANCEL_ID = ExecutingSideEffect.Id.Unique()

        @JvmStatic
        fun provideCancelOtherRules(): List<SideEffectTestCaseStrategy<TestSideEffect>> {
            return listOf(
                SideEffectTestCaseStrategy.create("onEnqueue") { target ->
                    executionRule {
                        onEnqueue {
                            cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onExecute") { target ->
                    executionRule {
                        onExecute {
                            cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onCancel", cancelRuleSideEffect()) { target ->
                    executionRule {
                        onCancel {
                            cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                }
            )
        }

        @JvmStatic
        fun provideSkipIfAlreadyExecutingRules(): List<SideEffectTestCaseStrategy<TestSideEffect?>> {
            return listOf(
                SideEffectTestCaseStrategy.create("onEnqueue") { target ->
                    executionRule {
                        onEnqueue {
                            skipIfAlreadyExecuting(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onExecute") { target ->
                    executionRule {
                        onExecute {
                            skipIfAlreadyExecuting(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                }
            )
        }

        @JvmStatic
        fun provideSendSignalRules(): List<SideEffectTestCaseStrategy<Any>> {
            return listOf(
                SideEffectTestCaseStrategy.create("onEnqueue") { signal ->
                    executionRule {
                        onEnqueue {
                            sendSignal(signal)
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onExecute") { signal ->
                    executionRule {
                        onExecute {
                            sendSignal(signal)
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onCancel", cancelRuleSideEffect()) { signal ->
                    executionRule {
                        onCancel {
                            sendSignal(signal)
                        }
                    }
                },
            )
        }

        @JvmStatic
        fun provideGetExecutingSideEffectsRules(): List<SideEffectTestCaseStrategy<MutableList<ExecutingSideEffect<TestSideEffect>>>> {
            return listOf(
                SideEffectTestCaseStrategy.create("onEnqueue") { collector ->
                    executionRule {
                        setId(ExecutingSideEffect.Id.Custom("check"))
                        onEnqueue {
                            collector.addAll(getExecutingSideEffects())
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onExecute") { collector ->
                    executionRule {
                        setId(ExecutingSideEffect.Id.Custom("check"))
                        onExecute {
                            collector.addAll(getExecutingSideEffects())
                        }
                    }
                },
                SideEffectTestCaseStrategy.create("onCancel", cancelRuleSideEffect()) { collector ->
                    executionRule {
                        onCancel {
                            collector.addAll(getExecutingSideEffects(InstanceFilter.Filter { it.id != CANCEL_ID }))
                        }
                    }
                },
            )
        }

        private fun cancelRuleSideEffect(): (
            handler: SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>,
            ruleSideEffect: TestSideEffect,
        ) -> Unit {
            return { handler, ruleSideEffect ->
                val cancelSignalSideEffect = TestSideEffect(executionRule {
                    setId(CANCEL_ID)
                    onExecute {
                        cancelOther(InstanceFilter.Filter { it.sideEffect == ruleSideEffect })
                    }
                })

                handler.onSideEffect(cancelSignalSideEffect)
            }
        }
    }
}