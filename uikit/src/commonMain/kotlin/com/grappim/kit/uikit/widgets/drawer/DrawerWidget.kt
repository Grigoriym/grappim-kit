package com.grappim.kit.uikit.widgets.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource

/**
 * Phone-width nav drawer, built from a caller-supplied [DrawerItem] list. [headerTitle] is
 * resolved by the caller (typically the app's own name) rather than bundled here — this module
 * has no localized strings of its own, same convention as [com.grappim.kit.uikit.widgets.topbar.TopBar]'s
 * caller-supplied content descriptions. See [NavigationSuiteWidget] for the tablet/expanded-width
 * counterpart.
 */
@Composable
fun <T> DrawerWidget(
    drawerItems: ImmutableList<DrawerItem<T>>,
    currentTopLevelDestination: T?,
    onDrawerItemClick: (T) -> Unit,
    drawerState: DrawerState,
    headerTitle: NativeText,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        modifier = modifier,
        gesturesEnabled = gesturesEnabled,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerState = drawerState) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = headerTitle.asString(),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    drawerItems.forEach { drawerItem ->
                        when (drawerItem) {
                            is DrawerItem.Group -> {
                                Text(
                                    text = drawerItem.label.asString(),
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                drawerItem.items.forEach { destination ->
                                    DrawerDestinationItem(
                                        item = destination,
                                        isSelected = currentTopLevelDestination == destination.destination,
                                        onClick = { onDrawerItemClick(destination.destination) }
                                    )
                                }
                            }

                            is DrawerItem.Destination -> {
                                DrawerDestinationItem(
                                    item = drawerItem,
                                    isSelected = currentTopLevelDestination == drawerItem.destination,
                                    onClick = { onDrawerItemClick(drawerItem.destination) }
                                )
                            }

                            is DrawerItem.Divider -> {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        content = content
    )
}

@Composable
private fun <T> DrawerDestinationItem(
    item: DrawerItem.Destination<T>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = item.label.asString()

    NavigationDrawerItem(
        modifier = modifier,
        label = { Text(text = label) },
        selected = isSelected,
        icon = {
            when (val iconSource = item.icon) {
                is IconSource.Vector -> Icon(
                    imageVector = iconSource.imageVector,
                    contentDescription = label
                )

                is IconSource.Resource -> Icon(
                    painter = painterResource(iconSource.resourceId),
                    contentDescription = label
                )
            }
        },
        onClick = onClick
    )
}
