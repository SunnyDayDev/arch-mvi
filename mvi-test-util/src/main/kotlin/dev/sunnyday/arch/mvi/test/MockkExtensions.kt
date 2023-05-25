package dev.sunnyday.arch.mvi.test

import io.mockk.Matcher
import io.mockk.MockKGateway
import io.mockk.MockKMatcherScope
import io.mockk.every
import kotlin.random.Random
import kotlin.reflect.KClass

fun MockKMatcherScope.anyProvider(): MockProvider {
    return AnyProvider(this)
}

inline fun <T : Any> mockkByClass(
    klass: KClass<T>,
    name: String? = null,
    relaxed: Boolean = true,
    relaxUnitFun: Boolean = true,
    config: T.() -> Unit = {},
): T {
    return MockKGateway.implementation().mockFactory.mockk(
        mockType = klass,
        name = name,
        relaxed = relaxed,
        moreInterfaces = emptyArray(),
        relaxUnitFun = relaxUnitFun,
    ).also(config)
}

fun <T : Any> constructedWithByClass(
    klass: KClass<T>,
    args: Array<Matcher<*>>? = null,
): T {
    return MockKGateway.implementation().constructorMockFactory.mockPlaceholder(
        klass,
        args = args,
    )
}

/**
 * It's [io.mockk.mockk], but with predefined mocks for object methods [Any.hashCode] and [Any.equals].
 */
inline fun <reified T : Any> stub(config: T.() -> Unit = {}): T {
    return stubByClass(T::class, config)
}

/**
 * It's [mockkByClass], but with predefined mocks for object methods [Any.hashCode] and [Any.equals].
 */
inline fun <T : Any> stubByClass(klass: KClass<T>, config: T.() -> Unit = {}): T {
    val hashCode = Random.nextInt()

    return mockkByClass(klass) {
        val mock = this

        every { mock.hashCode() } returns hashCode
        every { mock == any() } answers { mock === firstArg<Any?>() }

        config()
    }
}