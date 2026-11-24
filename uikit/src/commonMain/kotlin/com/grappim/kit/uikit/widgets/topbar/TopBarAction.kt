package com.grappim.kit.uikit.widgets.topbar

import androidx.compose.ui.graphics.vector.ImageVector
import com.grappim.kit.uikit.NativeText
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.DrawableResource

/**
 * An action slot in [TopBar]. The button variants each carry their own `onClick`;
 * [TopBarActionMenu] has none of its own — its button only opens the menu, and each
 * [TopBarMenuItem] carries the click instead.
 */
sealed interface TopBarAction

data class TopBarActionIconButton(
    val drawable: DrawableResource,
    val contentDescription: String = "",
    val enabled: Boolean = true,
    val onClick: () -> Unit
) : TopBarAction

data class TopBarActionVectorButton(
    val imageVector: ImageVector,
    val contentDescription: String = "",
    val enabled: Boolean = true,
    val onClick: () -> Unit
) : TopBarAction

data class TopBarActionTextButton(
    val text: NativeText,
    val enabled: Boolean = true,
    val onClick: () -> Unit
) : TopBarAction

/**
 * An icon button (typically `Icons.Filled.MoreVert`) that opens a dropdown of [items] anchored
 * to it. Selecting an item closes the menu, then calls that item's [TopBarMenuItem.onClick].
 * Whether the menu is open is local UI state inside [TopBar], not part of this config.
 */
data class TopBarActionMenu(
    val imageVector: ImageVector,
    val contentDescription: String,
    val items: ImmutableList<TopBarMenuItem>,
    val enabled: Boolean = true
) : TopBarAction

data class TopBarMenuItem(
    val text: NativeText,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/** Closes the menu before running the item's action, so the action may navigate away safely. */
internal fun TopBarMenuItem.select(dismissMenu: () -> Unit) {
    dismissMenu()
    onClick()
}
