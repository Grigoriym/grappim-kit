package com.grappim.kit.uikit.widgets.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Medium/expanded-width counterpart to [DrawerWidget], built on [NavigationSuiteScaffoldLayout] with a
 * flattened item list (see [flattenForNavigationSuite]). The caller picks [layoutType]:
 *
 * - [NavigationSuiteType.WideNavigationRailCollapsed] / [NavigationSuiteType.WideNavigationRailExpanded]
 *   render an expandable [WideNavigationRail] — Material's pattern for more than 7 destinations. A menu
 *   button in the rail's header toggles it. Collapsed, it shows only [DrawerItem.Destination.primary]
 *   destinations (plus the selected one); expanded, it shows every destination. [layoutType] sets the
 *   initial state and re-syncs it whenever it changes; the user's toggle survives configuration
 *   changes otherwise.
 * - Any other type (bars, the legacy `NavigationRail`/`NavigationDrawer`) renders every destination
 *   through the library's own [NavigationSuiteScaffold], ignoring `primary`. None of these scroll, so
 *   they suit short destination lists only.
 *
 * The wide rail's items sit in a vertical scroll container, so even an expanded rail taller than the
 * window keeps every destination reachable. Material3's rails don't scroll on their own.
 *
 * @param expandRailContentDescription / [collapseRailContentDescription] — the rail menu button's
 *   accessibility labels. Caller-supplied like every other content description in this module; the
 *   English defaults only exist to keep the pre-0.1.9 signature source-compatible.
 */
@Composable
fun <T> NavigationSuiteWidget(
    drawerItems: ImmutableList<DrawerItem<T>>,
    currentTopLevelDestination: T?,
    onDrawerItemClick: (T) -> Unit,
    layoutType: NavigationSuiteType,
    modifier: Modifier = Modifier,
    expandRailContentDescription: NativeText = NativeText.Simple("Expand navigation"),
    collapseRailContentDescription: NativeText = NativeText.Simple("Collapse navigation"),
    content: @Composable () -> Unit
) {
    val destinations = flattenForNavigationSuite(drawerItems)

    if (layoutType.isWideNavigationRail) {
        ExpandableRailScaffold(
            destinations = destinations,
            currentTopLevelDestination = currentTopLevelDestination,
            onDrawerItemClick = onDrawerItemClick,
            layoutType = layoutType,
            expandRailContentDescription = expandRailContentDescription,
            collapseRailContentDescription = collapseRailContentDescription,
            modifier = modifier,
            content = content
        )
    } else {
        NavigationSuiteScaffold(
            modifier = modifier,
            navigationSuiteType = layoutType,
            navigationItems = {
                destinations.forEach { destination ->
                    NavigationSuiteItem(
                        navigationSuiteType = layoutType,
                        selected = currentTopLevelDestination == destination.destination,
                        onClick = { onDrawerItemClick(destination.destination) },
                        icon = { DestinationIcon(destination) },
                        label = { Text(destination.label.asString()) }
                    )
                }
            },
            content = content
        )
    }
}

/**
 * Mirrors what [NavigationSuiteScaffold]'s default override does (Surface + [NavigationSuiteScaffoldLayout]
 * + start-inset consumption), but supplies its own [WideNavigationRail]: the scaffold's built-in one has
 * no hoisted [WideNavigationRailState], so its header can't expand/collapse it.
 */
@Composable
private fun <T> ExpandableRailScaffold(
    destinations: List<DrawerItem.Destination<T>>,
    currentTopLevelDestination: T?,
    onDrawerItemClick: (T) -> Unit,
    layoutType: NavigationSuiteType,
    expandRailContentDescription: NativeText,
    collapseRailContentDescription: NativeText,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val initiallyExpanded = layoutType == NavigationSuiteType.WideNavigationRailExpanded
    val railState = rememberWideNavigationRailState(
        initialValue = if (initiallyExpanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed
    )
    // Re-sync when the caller switches between the two wide-rail types (e.g. on a window-size-class
    // change). A no-op on first composition, since the initial value already matches.
    LaunchedEffect(layoutType) {
        if (initiallyExpanded) railState.expand() else railState.collapse()
    }

    Surface(
        modifier = modifier,
        color = NavigationSuiteScaffoldDefaults.containerColor,
        contentColor = NavigationSuiteScaffoldDefaults.contentColor
    ) {
        NavigationSuiteScaffoldLayout(
            navigationSuiteType = layoutType,
            navigationSuite = {
                ExpandableRail(
                    destinations = destinations,
                    currentTopLevelDestination = currentTopLevelDestination,
                    onDrawerItemClick = onDrawerItemClick,
                    railState = railState,
                    expandRailContentDescription = expandRailContentDescription,
                    collapseRailContentDescription = collapseRailContentDescription
                )
            },
            content = {
                Box(
                    Modifier.consumeWindowInsets(
                        WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                    )
                ) {
                    content()
                }
            }
        )
    }
}

@Composable
private fun <T> ExpandableRail(
    destinations: List<DrawerItem.Destination<T>>,
    currentTopLevelDestination: T?,
    onDrawerItemClick: (T) -> Unit,
    railState: WideNavigationRailState,
    expandRailContentDescription: NativeText,
    collapseRailContentDescription: NativeText
) {
    val scope = rememberCoroutineScope()
    val expanded = railState.targetValue == WideNavigationRailValue.Expanded

    // WideNavigationRailLayout spaces and min-sizes each of its direct children. Wrapping the items in
    // one scroll Column hands it a single child instead, so the same per-item values (and their
    // collapse/expand animation) are re-applied here. Values from material3 1.10.0-alpha05's
    // WideNavigationRail.kt: NavigationRailCollapsedTokens.ItemVerticalSpace (4dp) and
    // NavigationRailBaselineItemTokens.ContainerHeight (64dp) collapsed; 0dp and the minimum
    // interactive size expanded.
    val minimumInteractiveSize = LocalMinimumInteractiveComponentSize.current
        .takeIf { it != Dp.Unspecified } ?: 0.dp
    val itemSpacing by animateDpAsState(if (expanded) 0.dp else CollapsedItemVerticalSpace)
    val itemMinHeight by animateDpAsState(if (expanded) minimumInteractiveSize else CollapsedItemMinHeight)

    WideNavigationRail(
        state = railState,
        colors = NavigationSuiteDefaults.colors().wideNavigationRailColors,
        header = {
            IconButton(
                modifier = Modifier.padding(start = RailMenuButtonStartPadding),
                onClick = { scope.launch { railState.toggle() } }
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = if (expanded) {
                        collapseRailContentDescription.asString()
                    } else {
                        expandRailContentDescription.asString()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                // The rail places its content WNRHeaderPadding (40dp) below the header but measures it
                // against the full height under the header, so a full-height child would overflow the
                // rail's bottom by that much. Shrinking the viewport by the same amount keeps the last
                // item scrollable into view instead of hidden behind the bottom edge.
                .padding(bottom = RailHeaderPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            destinations.forEach { destination ->
                AnimatedVisibility(
                    visible = isShownInRail(destination, expanded, currentTopLevelDestination),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    WideNavigationRailItem(
                        modifier = Modifier.heightIn(min = itemMinHeight),
                        railExpanded = expanded,
                        selected = currentTopLevelDestination == destination.destination,
                        onClick = { onDrawerItemClick(destination.destination) },
                        icon = { DestinationIcon(destination) },
                        label = { Text(destination.label.asString()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> DestinationIcon(destination: DrawerItem.Destination<T>) {
    val contentDescription = destination.label.asString()
    when (val iconSource = destination.icon) {
        is IconSource.Vector -> Icon(
            imageVector = iconSource.imageVector,
            contentDescription = contentDescription
        )

        is IconSource.Resource -> Icon(
            painter = painterResource(iconSource.resourceId),
            contentDescription = contentDescription
        )
    }
}

private val NavigationSuiteType.isWideNavigationRail: Boolean
    get() = this == NavigationSuiteType.WideNavigationRailCollapsed ||
        this == NavigationSuiteType.WideNavigationRailExpanded

private val CollapsedItemVerticalSpace = 4.dp
private val CollapsedItemMinHeight = 64.dp
private val RailHeaderPadding = 40.dp

// Aligns the menu button's icon with the collapsed items' icons: the 96dp rail centers a 56dp-wide
// indicator (20dp each side), and the 48dp IconButton centers its 24dp icon (12dp each side).
private val RailMenuButtonStartPadding = 24.dp
