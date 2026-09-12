package com.grappim.kit.uikit.widgets.drawer

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grappim.kit.uikit.asString
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource

/**
 * Medium/expanded-width counterpart to [DrawerWidget] — renders a rail or permanent drawer via
 * [NavigationSuiteScaffold] using a flattened item list (see [flattenForNavigationSuite]).
 */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
fun <T> NavigationSuiteWidget(
    drawerItems: ImmutableList<DrawerItem<T>>,
    currentTopLevelDestination: T?,
    onDrawerItemClick: (T) -> Unit,
    layoutType: NavigationSuiteType,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val destinations = flattenForNavigationSuite(drawerItems)

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        navigationSuiteItems = {
            destinations.forEach { destination ->
                item(
                    selected = currentTopLevelDestination == destination.destination,
                    onClick = { onDrawerItemClick(destination.destination) },
                    icon = {
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
                    },
                    label = { Text(destination.label.asString()) }
                )
            }
        },
        content = content
    )
}
