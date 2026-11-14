package com.grappim.kit.customheaders

/**
 * Enforces OkHttp's rule rather than Ktor's: the engine is the stricter of the two, and it throws
 * at request time — after the user has left the screen that could have said why.
 *
 * A row with both fields blank is not an error: it is an unfilled row, and it is never sent.
 *
 * [reservedNames] is per app: pass [DEFAULT_RESERVED_NAMES] plus whatever headers the app sets
 * itself (e.g. `Authorization` when it sends its own bearer token, `User-Agent` when it sets one
 * per request). Compared ignoring case.
 */
class CustomHeaderValidator(private val reservedNames: List<String> = DEFAULT_RESERVED_NAMES) {

    /** One entry per row of [headers], in order — `null` for a row that can be sent. */
    fun validate(headers: List<CustomHeader>): List<CustomHeaderError?> {
        val seen = mutableSetOf<String>()
        return headers.map { header ->
            if (header.isBlank()) return@map null
            validateName(header.name) ?: validateValue(header.value)
                ?: CustomHeaderError.DuplicateName.takeUnless { seen.add(header.name.lowercase()) }
        }
    }

    private fun validateName(name: String): CustomHeaderError? = when {
        name.isEmpty() -> CustomHeaderError.MissingName
        name.any { it.code !in NAME_CHARS || it in DELIMITERS } -> CustomHeaderError.InvalidName
        reservedNames.any { it.equals(name, ignoreCase = true) } -> CustomHeaderError.ReservedName
        else -> null
    }

    private fun validateValue(value: String): CustomHeaderError? =
        CustomHeaderError.InvalidValue.takeIf { value.any { it != '\t' && it.code !in VALUE_CHARS } }

    companion object {
        /**
         * Names the HTTP stack owns. `Transfer-Encoding`/`Upgrade` are Ktor's `UnsafeHeadersList`
         * (the engine throws on them); `Cookie` would replace a web-login session.
         */
        val DEFAULT_RESERVED_NAMES: List<String> = listOf(
            "Host",
            "Content-Length",
            "Content-Type",
            "Transfer-Encoding",
            "Upgrade",
            "Connection",
            "Cookie"
        )

        private val NAME_CHARS = 0x21..0x7E
        private val VALUE_CHARS = 0x20..0x7E
        private const val DELIMITERS = "\"(),/:;<=>?@[\\]{}"
    }
}
