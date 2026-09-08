package com.grappim.kit.uikit

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [NativeText.asString] is `@Composable` and is not covered here — only its non-composable twin
 * [NativeText.asStringBlocking] is. The two `when` bodies are written separately, so this proves
 * nothing about the composable one. `Resource`/`Arguments`/`Plural` aren't exercised either: they
 * need a real [org.jetbrains.compose.resources.StringResource], which only a resource-generating
 * module (i.e. a consuming app, not this one) can construct.
 */
class NativeTextTest {

    private fun randomString(): String = List(15) { Random.nextInt(97, 123).toChar() }.joinToString("")

    @Test
    fun `only Empty is empty`() {
        assertTrue(NativeText.Empty.isEmpty())
        assertFalse(NativeText.Empty.isNotEmpty())

        val nonEmpty = listOf(
            NativeText.Simple(randomString()),
            NativeText.Multi(emptyList())
        )
        nonEmpty.forEach {
            assertFalse(it.isEmpty(), "$it should not be empty")
            assertTrue(it.isNotEmpty(), "$it should be not empty")
        }
    }

    @Test
    fun `asStringBlocking returns the text of Simple`() {
        val text = randomString()

        assertEquals(text, NativeText.Simple(text).asStringBlocking())
    }

    @Test
    fun `asStringBlocking returns an empty string for Empty`() {
        assertEquals("", NativeText.Empty.asStringBlocking())
    }

    @Test
    fun `asStringBlocking concatenates Multi in order`() {
        val first = randomString()
        val second = randomString()
        val multi = NativeText.Multi(
            listOf(
                NativeText.Simple(first),
                NativeText.Empty,
                NativeText.Simple(second)
            )
        )

        assertEquals(first + second, multi.asStringBlocking())
    }

    @Test
    fun `asStringBlocking resolves nested Multi`() {
        val inner = randomString()
        val outer = randomString()
        val multi = NativeText.Multi(
            listOf(
                NativeText.Multi(listOf(NativeText.Simple(inner))),
                NativeText.Simple(outer)
            )
        )

        assertEquals(inner + outer, multi.asStringBlocking())
    }

    @Test
    fun `asStringBlocking of an empty Multi is an empty string`() {
        assertEquals("", NativeText.Multi(emptyList()).asStringBlocking())
    }
}
