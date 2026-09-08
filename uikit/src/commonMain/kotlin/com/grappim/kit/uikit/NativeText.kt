package com.grappim.kit.uikit

import androidx.compose.runtime.Composable
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A string a non-UI layer (a ViewModel, a domain error mapper) can produce without touching a
 * resource loader: it picks the variant, a Composable resolves it later with [asString]. The
 * consuming app supplies every [StringResource]/[PluralStringResource] — this type only ever
 * holds a pointer to one, never generates or bundles resources itself.
 */
sealed class NativeText {
    data object Empty : NativeText()

    data class Simple(val text: String) : NativeText()

    data class Resource(val stringResource: StringResource) : NativeText()

    data class Arguments(val stringResource: StringResource, val args: List<Any>) : NativeText()

    data class Plural(val pluralStringResource: PluralStringResource, val number: Int, val args: List<Any>) :
        NativeText()

    data class Multi(val text: List<NativeText>) : NativeText()

    fun isEmpty(): Boolean = this is Empty

    fun isNotEmpty(): Boolean = this !is Empty
}

@Suppress("SpreadOperator")
@Composable
fun NativeText.asString(): String = when (this) {
    is NativeText.Empty -> ""
    is NativeText.Simple -> text
    is NativeText.Resource -> stringResource(stringResource)
    is NativeText.Arguments -> stringResource(stringResource, *args.toTypedArray())
    is NativeText.Plural -> pluralStringResource(pluralStringResource, number, *args.toTypedArray())
    is NativeText.Multi -> buildString { text.forEach { append(it.asString()) } }
}

/**
 * Non-composable version for use inside lambdas (onClick, snackbar callbacks, etc.) where a
 * `@Composable` function cannot be called.
 */
@Suppress("SpreadOperator")
fun NativeText.asStringBlocking(): String = when (this) {
    is NativeText.Empty -> ""
    is NativeText.Simple -> text
    is NativeText.Resource -> runBlocking { getString(stringResource) }
    is NativeText.Arguments -> runBlocking { getString(stringResource, *args.toTypedArray()) }
    is NativeText.Plural -> runBlocking { getPluralString(pluralStringResource, number, *args.toTypedArray()) }
    is NativeText.Multi -> text.joinToString("") { it.asStringBlocking() }
}
