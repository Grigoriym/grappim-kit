package com.grappim.kit.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Named [CoroutineDispatcher]s, ported from each app's own Koin-annotated qualifiers --
 * plain here so this module doesn't need to pull in a DI framework. Bind these to your
 * own Koin/Hilt qualifiers in the consuming app. */
object KitDispatchers {
    val default: CoroutineDispatcher get() = Dispatchers.Default
    val io: CoroutineDispatcher get() = Dispatchers.IO
    val main: CoroutineDispatcher get() = Dispatchers.Main
    val mainImmediate: CoroutineDispatcher get() = Dispatchers.Main.immediate
}
