package com.grappim.kit.customheaders

import kotlinx.coroutines.flow.Flow

/**
 * The headers a reverse proxy in front of the app's server asks for. They belong to the *server*,
 * like its URL, so an app's logout/credential reset should leave them alone.
 *
 * [currentHeaders] is deliberately **not** `suspend`: it is read per request from a Ktor plugin
 * hook that can't suspend for it, so the implementation keeps the value cached.
 */
interface CustomHeadersStorage {

    /** Emits the stored list, empty until something is saved, and again on every change. */
    val headers: Flow<List<CustomHeader>>

    val currentHeaders: List<CustomHeader>

    suspend fun setHeaders(headers: List<CustomHeader>)
}
