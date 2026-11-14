package com.grappim.kit.testing

import com.grappim.kit.customheaders.CustomHeader
import com.grappim.kit.customheaders.CustomHeadersStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCustomHeadersStorage(initial: List<CustomHeader> = emptyList()) : CustomHeadersStorage {

    private val state = MutableStateFlow(initial)

    /** Every list passed to [setHeaders], in order — including calls that then threw. */
    val setHeadersCalledWith = mutableListOf<List<CustomHeader>>()

    /** When set, [setHeaders] records the call and then throws this instead of saving. */
    var setHeadersThrows: Throwable? = null

    override val headers: Flow<List<CustomHeader>> = state.asStateFlow()

    override val currentHeaders: List<CustomHeader>
        get() = state.value

    override suspend fun setHeaders(headers: List<CustomHeader>) {
        setHeadersCalledWith += headers
        setHeadersThrows?.let { throw it }
        state.value = headers
    }
}
