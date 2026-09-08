package com.grappim.kit.uikit.widgets.topbar

import com.grappim.kit.uikit.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Controls the global state of the top bar. Each screen has its own config.
 */
data class TopBarConfig(
    val title: NativeText = NativeText.Empty,
    val subtitle: NativeText = NativeText.Empty,
    val navigationIcon: NavigationIconConfig = NavigationIconConfig.None,
    val actions: ImmutableList<TopBarAction> = persistentListOf()
)
