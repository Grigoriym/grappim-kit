package com.grappim.kit.placeholder

/**
 * Bootstrap-only. Exists to give the grappim-kit -> Maven Central publish pipeline
 * something to publish while checklist step 7 is verified; deleted once a real module
 * (core/navigation, step 8) publishes instead.
 */
public object Placeholder {
    public const val VERSION: String = "0.1.0"
}
