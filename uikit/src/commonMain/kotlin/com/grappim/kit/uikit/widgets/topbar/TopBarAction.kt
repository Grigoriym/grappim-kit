package com.grappim.kit.uikit.widgets.topbar

import androidx.compose.ui.graphics.vector.ImageVector
import com.grappim.kit.uikit.NativeText
import org.jetbrains.compose.resources.DrawableResource

sealed interface TopBarAction {
    val onClick: () -> Unit
}

data class TopBarActionIconButton(
    val drawable: DrawableResource,
    val contentDescription: String = "",
    val enabled: Boolean = true,
    override val onClick: () -> Unit
) : TopBarAction

data class TopBarActionVectorButton(
    val imageVector: ImageVector,
    val contentDescription: String = "",
    val enabled: Boolean = true,
    override val onClick: () -> Unit
) : TopBarAction

data class TopBarActionTextButton(
    val text: NativeText,
    val enabled: Boolean = true,
    override val onClick: () -> Unit
) : TopBarAction
