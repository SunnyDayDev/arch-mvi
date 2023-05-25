package dev.sunnyday.arch.mvi.test

import io.mockk.Matcher
import io.mockk.MockKGateway
import io.mockk.every
import io.mockk.verify
import kotlin.reflect.KClass

@Suppress("UnusedEquals")
class ConstructorRule<T : Any>(
    private val marker: Any,
    private val matchers: Array<Matcher<*>>,
    private val klass: KClass<T>,
) {

    fun verifyConstructorCalled(obj: Any, transformMatchers: ((Array<Matcher<*>>) -> Unit)? = null) {
        val matchers = if (transformMatchers != null) {
            matchers.copyOf().also(transformMatchers)
        } else {
            matchers
        }

        // mark equals(..) for verify,
        // if constructedWithByClass pass verify so  obj was constructed by constructor with same matchers
        obj == marker

        // hashCode calls in constructedWithByClass
        // since we call constructedWithByClass in verify, we have to be sure that we call hashCode previously
        matchers.forEach { it.hashCode() }

        verify {
            constructedWithByClass(klass, matchers) == marker
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