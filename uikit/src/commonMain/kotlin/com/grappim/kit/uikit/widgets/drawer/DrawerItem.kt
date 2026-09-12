package com.grappim.kit.uikit.widgets.drawer

import com.grappim.kit.uikit.NativeText
import kotlinx.collections.immutable.ImmutableList

/**
 * The drawer renders a list of these rather than a flat list of destinations, so a group of
 * related destinations can be shown under a header without the widget itself knowing about
 * grouping. [T] is the caller's own top-level-route type (typically an enum) — this module never
 * constructs or compares one, it only carries it through to [DrawerWidget]'s
 * `onDrawerItemClick`/`currentTopLevelDestination`.
 */
sealed interface DrawerItem<out T> {
    data class Group<T>(val label: NativeText, val items: List<Destination<T>>) : DrawerItem<T>

    data class Destination<T>(val destination: T, val label: NativeText, val icon: IconSource) : DrawerItem<T>

    data object Divider : DrawerItem<Nothing>
}

/**
 * Flattens grouped drawer items for [androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold],
 * whose flat `item()` API has no header/group-label/divider slot: [DrawerItem.Group] is unwrapped to its inner
 * destinations, [DrawerItem.Divider] is dropped, item order is otherwise preserved.
 */
fun <T> flattenForNavigationSuite(items: ImmutableList<DrawerItem<T>>): List<DrawerItem.Destination<T>> =
    items.flatMap { item ->
        when (item) {
            is DrawerItem.Destination -> listOf(item)
            is DrawerItem.Group -> item.items
            is DrawerItem.Divider -> emptyList()
        }
    }
