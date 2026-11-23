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

    /**
     * @param primary whether this destination stays visible while [NavigationSuiteWidget]'s wide
     *   navigation rail is collapsed. Material caps a collapsed rail at 7 destinations; mark at most
     *   that many as primary and the rest `false` — they appear once the rail is expanded via its
     *   header menu button. Defaults to `true`, so a list that never sets it shows every
     *   destination in both states. Ignored by [DrawerWidget] and by non-rail layout types.
     */
    data class Destination<T>(
        val destination: T,
        val label: NativeText,
        val icon: IconSource,
        val primary: Boolean = true
    ) : DrawerItem<T>

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

/**
 * Whether [destination] is shown by [NavigationSuiteWidget]'s wide navigation rail: every destination
 * when expanded; only [DrawerItem.Destination.primary] ones when collapsed — plus the currently
 * selected destination, so collapsing the rail while on a secondary destination never hides the
 * selection indicator.
 */
internal fun <T> isShownInRail(
    destination: DrawerItem.Destination<T>,
    railExpanded: Boolean,
    currentTopLevelDestination: T?
): Boolean = railExpanded || destination.primary || destination.destination == currentTopLevelDestination
