package dev.sunnyday.arch.mvi.test

import io.mockk.MockKGateway
import io.mockk.MockKMatcherScope
import kotlin.reflect.KClass

fun MockKMatcherScope.anyProvider(): MockProvider {
    return AnyProvider(this)
}

fun <T : Any> mockkByClass(klass: KClass<T>): T {
    return MockKGateway.implementation().mockFactory.mockk(
        mockType = klass,
        name = null,
        relaxed = true,
        moreInterfaces = emptyArray(),
        relaxUnitFun = true,
    )
}