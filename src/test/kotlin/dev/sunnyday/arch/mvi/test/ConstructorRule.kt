package dev.sunnyday.arch.mvi.test

import io.mockk.Matcher
import io.mockk.MockKGateway
import io.mockk.every
import kotlin.reflect.KClass

@Suppress("UnusedEquals")
class ConstructorRule<T: Any>(
    private val marker: Any,
    private val matchers: Array<Matcher<*>>,
    private val klass: KClass<T>,
) {

    fun verifyConstructorCalled(obj: Any) {
        obj == marker
        io.mockk.verify {
            MockKGateway.implementation().constructorMockFactory.mockPlaceholder(
                klass,
                args = matchers
            ) == marker
        }
    }

    companion object {

        inline fun <reified T : Any> create(vararg matchers: Matcher<*>): ConstructorRule<T> {
            val marker = Any()

            every { constructedWith<T>(*matchers) == marker } returns false

            @Suppress("UNCHECKED_CAST")
            return ConstructorRule(marker, matchers as Array<Matcher<*>>, T::class)
        }

        @Suppress("TestFunctionName")
        fun <T> Matcher(match: (T) -> Boolean): Matcher<T> = object : Matcher<T> {
            override fun match(arg: T?): Boolean = arg?.let(match) ?: false
        }
    }
}