package com.grappim.kit.uikit.widgets.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import com.grappim.kit.uikit.asString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Global top bar for the app. [backContentDescription]/[menuContentDescription] are resolved by
 * the caller (typically the one shell composable that calls [TopBar]) rather than bundled here —
 * this module has no localized strings of its own, every screen-supplied content description
 * ([NavigationIconConfig.Custom], every [TopBarAction]) already worked this way.
 */
// TODO: Material3 doesn't expose a subtitle slot yet (still alpha as of this port) -- revisit
// when it does, this Column-based workaround can go.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isVisible: Boolean,
    drawerState: DrawerState,
    topBarConfig: TopBarConfig,
    defaultGoBack: () -> Unit,
    backContentDescription: String,
    menuContentDescription: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        CenterAlignedTopAppBar(
            modifier = modifier,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = topBarConfig.title.asString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (topBarConfig.subtitle.isNotEmpty()) {
                        Text(
                            text = topBarConfig.subtitle.asString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            navigationIcon = {
                NavigationIcon(
                    navigationIconConfig = topBarConfig.navigationIcon,
                    drawerState = drawerState,
                    defaultGoBack = defaultGoBack,
                    backContentDescription = backContentDescription,
                    menuContentDescription = menuContentDescription
                )
            },
            actions = {
                topBarConfig.actions.forEach { action ->
                    when (action) {
                        is TopBarActionIconButton -> {
                            IconButton(onClick = action.onClick, enabled = action.enabled) {
                                Icon(
                                    painter = painterResource(action.drawable),
                                    contentDescription = action.contentDescription
                                )
                            }
                        }

                        is TopBarActionVectorButton -> {
                            IconButton(onClick = action.onClick, enabled = action.enabled) {
                                Icon(
                                    imageVector = action.imageVector,
                                    contentDescription = action.contentDescription
                                )
                            }
                        }

                        is TopBarActionTextButton -> {
                            TextButton(onClick = action.onClick, enabled = action.enabled) {
                                Text(text = action.text.asString())
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun NavigationIcon(
    navigationIconConfig: NavigationIconConfig,
    drawerState: DrawerState,
    defaultGoBack: () -> Unit,
    backContentDescription: String,
    menuContentDescription: String
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    when (navigationIconConfig) {
        NavigationIconConfig.None -> {}

        is NavigationIconConfig.Back -> {
            IconButton(onClick = navigationIconConfig.onBackClick ?: defaultGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backContentDescription)
            }
        }

        NavigationIconConfig.Menu -> {
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    scope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                }
            ) {
                Icon(Icons.Filled.Menu, contentDescription = menuContentDescription)
            }
        }

        is NavigationIconConfig.Custom -> {
            IconButton(onClick = navigationIconConfig.onClick) {
                Icon(
                    painter = painterResource(navigationIconConfig.icon),
                    contentDescription = navigationIconConfig.contentDescription
                )
            }
        }
    }
}
