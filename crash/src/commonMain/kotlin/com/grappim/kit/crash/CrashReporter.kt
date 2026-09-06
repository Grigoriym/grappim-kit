package com.grappim.kit.crash

/**
 * A seam a lower Gradle module can depend on without pulling in a concrete crash-reporting SDK
 * or its platform-specific implementation -- the implementation is platform glue and arrives
 * with the DI wiring in the app's entry-point module.
 */
interface CrashReporter {

    val isAvailable: Boolean

    fun setCollectionEnabled(enabled: Boolean)

    fun recordException(throwable: Throwable)

    fun log(message: String)
}
