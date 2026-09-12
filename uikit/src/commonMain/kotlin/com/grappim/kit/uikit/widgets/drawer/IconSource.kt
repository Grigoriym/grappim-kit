package com.grappim.kit.uikit.widgets.drawer

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource

sealed interface IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource

    data class Resource(val resourceId: DrawableResource) : IconSource
}
