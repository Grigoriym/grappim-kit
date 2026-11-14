package com.grappim.kit.customheaders

import kotlin.test.Test
import kotlin.test.assertEquals

class CustomHeaderValidatorTest {

    private val sut = CustomHeaderValidator()

    private fun errorFor(name: String, value: String = "x", validator: CustomHeaderValidator = sut) =
        validator.validate(listOf(CustomHeader(name, value))).single()

    @Test
    fun `the headers proxies actually ask for are accepted`() {
        val headers = listOf(
            CustomHeader("Authorization", "Basic cGFuZ29saW46c2VjcmV0"),
            CustomHeader("CF-Access-Client-Id", "abc.access"),
            CustomHeader("X-Token", "a b\tc")
        )

        assertEquals(listOf(null, null, null), sut.validate(headers))
    }

    @Test
    fun `an unfilled row is not an error`() {
        assertEquals(listOf(null), sut.validate(listOf(CustomHeader(" ", ""))))
    }

    @Test
    fun `a value without a name is MissingName`() {
        assertEquals(CustomHeaderError.MissingName, errorFor(name = "", value = "secret"))
    }

    @Test
    fun `names refuse whitespace and delimiters and non-ascii`() {
        listOf("X Token", "X:Token", "X(Token)", "Tökén", "X/Token").forEach { name ->
            assertEquals(CustomHeaderError.InvalidName, errorFor(name), name)
        }
    }

    /** OkHttp throws on non-ASCII at request time, so it has to be caught here. */
    @Test
    fun `values refuse control characters and non-ascii`() {
        listOf("a\nb", "a\rb", "a\u0000b", "pässword").forEach { value ->
            assertEquals(CustomHeaderError.InvalidValue, errorFor("X-Token", value), value)
        }
    }

    @Test
    fun `an empty value is allowed`() {
        assertEquals(null, errorFor("X-Token", ""))
    }

    @Test
    fun `the default reserved names are refused ignoring case`() {
        listOf("Host", "content-length", "Content-Type", "transfer-encoding", "Upgrade", "Connection", "COOKIE")
            .forEach { name -> assertEquals(CustomHeaderError.ReservedName, errorFor(name), name) }
    }

    @Test
    fun `app-supplied reserved names are refused on top of the defaults`() {
        val validator = CustomHeaderValidator(
            CustomHeaderValidator.DEFAULT_RESERVED_NAMES + listOf("Authorization", "User-Agent")
        )

        listOf("authorization", "User-Agent", "Host").forEach { name ->
            assertEquals(CustomHeaderError.ReservedName, errorFor(name, validator = validator), name)
        }
        assertEquals(null, errorFor("X-Token", validator = validator))
    }

    @Test
    fun `a repeated name is DuplicateName on the repeat only ignoring case`() {
        val headers = listOf(CustomHeader("X-Token", "a"), CustomHeader("x-token", "b"), CustomHeader("X-Other", "c"))

        assertEquals(listOf(null, CustomHeaderError.DuplicateName, null), sut.validate(headers))
    }
}
