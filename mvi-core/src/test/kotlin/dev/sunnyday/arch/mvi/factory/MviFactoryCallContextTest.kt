package dev.sunnyday.arch.mvi.factory

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MviFactoryCallContextTest {

    @Test
    fun `factory call context doesn't exists out of the call context`() {
        assertNull(MviFactoryCallContext.getCurrentFactoryContext())
        MviFactoryCallContext().runWithFactoryContext { }
        assertNull(MviFactoryCallContext.getCurrentFactoryContext())
    }

    @Test
    fun `factory call context provide self in self scope`() {
        val expectedContext = MviFactoryCallContext()
        var actualContext: MviFactoryCallContext? = null

        expectedContext.runWithFactoryContext { actualContext = MviFactoryCallContext.getCurrentFactoryContext() }

        assertSame(expectedContext, actualContext)
    }

    @Test
    fun `factory contexts with equal content are equals`() {
        val parentA = MviFactoryCallContext.create(ContextContainer("parent"))
        val contextA = MviFactoryCallContext.create(ContextContainer("context"))
        val actualContextA = runContextChain(parentA, contextA)

        val parentB = MviFactoryCallContext.create(ContextContainer("parent"))
        val contextB = MviFactoryCallContext.create(ContextContainer("context"))
        val actualContextB = runContextChain(parentB, contextB)

        assertNotNull(actualContextA)
        assertEquals(actualContextA, actualContextB)
        assertEquals(parentA, parentB)
        assertEquals(actualContextA.hashCode(), actualContextB.hashCode())
        assertEquals(parentA.hashCode(), parentB.hashCode())
    }

    @Test
    fun `factory contexts are not equal if parents are not equals`() {
        val parentA = MviFactoryCallContext.create(ContextContainer("parentA"))
        val contextA = MviFactoryCallContext.create(ContextContainer("context"))
        val actualContextA = runContextChain(parentA, contextA)

        val parentB = MviFactoryCallContext.create(ContextContainer("parentB"))
        val contextB = MviFactoryCallContext.create(ContextContainer("context"))
        val actualContextB = runContextChain(parentB, contextB)

        assertNotNull(actualContextA)
        assertNotEquals(actualContextA, actualContextB)
    }

    @Test
    fun `factory contexts are not equal if content isn't equal`() {
        val parentA = MviFactoryCallContext.create(ContextContainer("parent"))
        val contextA = MviFactoryCallContext.create(ContextContainer("contextA"))
        val actualContextA = runContextChain(parentA, contextA)

        val parentB = MviFactoryCallContext.create(ContextContainer("parent"))
        val contextB = MviFactoryCallContext.create(ContextContainer("contextB"))
        val actualContextB = runContextChain(parentB, contextB)

        assertNotNull(actualContextA)
        assertNotEquals(actualContextA, actualContextB)
    }

    @Test
    fun `get element from context`() {
        val context = runContextChain(
            MviFactoryCallContext.create(ContextContainer("parent")),
            MviFactoryCallContext.create(ContextContainer("contextA")),
        )

        assertEquals(ContextContainer("contextA"), context[ContextContainer])
    }

    @Test
    fun `get element from parent context if not exists`() {
        val context = runContextChain(
            MviFactoryCallContext.create(ParentContextContainer("parent")),
            MviFactoryCallContext.create(ContextContainer("contextA")),
        )

        assertEquals(ParentContextContainer("parent"), context[ParentContextContainer])
    }

    @Test
    fun `factory call context are not equal to other types`() {
        assertFalse(MviFactoryCallContext().equals(ContextContainer("any")))
    }

    @Test
    fun `if element doesn't exist in context return null`() {
        MviFactoryCallContext.create(ContextContainer("some"))[ParentContextContainer]
            .let(::assertNull)
    }


    @Test
    fun `if already in context scope don't wrap more`() {
        val context = MviFactoryCallContext()

        val actualContext = runContextChain(context, context)

        assertEquals(context, actualContext)
    }

    @Test
    fun `to string describes context content`() {
        val context = runContextChain(
            MviFactoryCallContext.create(ParentContextContainer("parent")),
            MviFactoryCallContext.create(ContextContainer("contextA")),
        )

        assertEquals("ParentContextContainer(value=parent) <- ContextContainer(value=contextA)", context.toString())
    }

    @Test
    fun `get element by self key return self`() {
        val element = ContextContainer("ctx")
        assertSame(element, element[ContextContainer.Key])
    }

    @Test
    fun `get element by other key return null`() {
        val element = ContextContainer("ctx")
        assertNull(element[ParentContextContainer.Key])
    }

    private fun runContextChain(vararg contexts: MviFactoryCallContext): MviFactoryCallContext {
        if (contexts.isEmpty()) return MviFactoryCallContext.requireCurrentFactoryContext().clone()

        return contexts.first().runWithFactoryContext {
            runContextChain(*contexts.drop(1).toTypedArray())
        }
    }

    private data class ContextContainer(val value: Any) : MviFactoryCallContext.Element {

        override val key: MviFactoryCallContext.Key<*>
            get() = Key

        companion object Key : MviFactoryCallContext.Key<ContextContainer>
    }

    private data class ParentContextContainer(val value: Any) : MviFactoryCallContext.Element {

        override val key: MviFactoryCallContext.Key<*>
            get() = Key

        companion object Key : MviFactoryCallContext.Key<ParentContextContainer>
    }
}