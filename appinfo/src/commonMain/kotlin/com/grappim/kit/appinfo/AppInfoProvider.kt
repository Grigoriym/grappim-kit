package com.grappim.kit.appinfo

/**
 * Build-time facts the shared code can't read for itself. The implementation is platform glue
 * and arrives with the DI wiring in the app's entry-point module.
 *
 * Takes raw fields rather than a rendered string on purpose, so formatting stays in the `ui`
 * layer where a resource can carry it.
 */
interface AppInfoProvider {
    fun isDebug(): Boolean

    fun isFdroidBuild(): Boolean

    fun versionName(): String

    fun versionCode(): Int

    fun buildType(): String
}
