package dev.sunnyday.arch.mvi.coroutine.test

import dev.sunnyday.arch.mvi.coroutine.primitive.CoroutineObservableEvent
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.test.CoroutineTestUtil
import kotlinx.coroutines.flow.Flow

class CoroutineObservableEventFlowProvider : CoroutineTestUtil.FlowProvider {

    override fun <T : Any> getFlow(observable: ObservableEvent<T>): Flow<T>? {
        return (observable as? CoroutineObservableEvent)?.flow
    }
}