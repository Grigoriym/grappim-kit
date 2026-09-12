package com.grappim.kit.uikit.widgets.drawer

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.grappim.kit.uikit.NativeText
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

private enum class TestDestination { Dashboard, ScrumBacklog, ScrumClosedSprints, Settings }

private val icon = IconSource.Vector(
    ImageVector.Builder(defaultWidth = Dp(1f), defaultHeight = Dp(1f), viewportWidth = 1f, viewportHeight = 1f).build()
)

class DrawerItemTest {

    private val dashboard = DrawerItem.Destination(
        destination = TestDestination.Dashboard,
        label = NativeText.Simple("Dashboard"),
        icon = icon
    )
    private val scrumBacklog = DrawerItem.Destination(
        destination = TestDestination.ScrumBacklog,
        label = NativeText.Simple("Backlog"),
        icon = icon
    )
    private val scrumClosedSprints = DrawerItem.Destination(
        destination = TestDestination.ScrumClosedSprints,
        label = NativeText.Simple("Closed sprints"),
        icon = icon
    )
    private val settingsItem = DrawerItem.Destination(
        destination = TestDestination.Settings,
        label = NativeText.Simple("Settings"),
        icon = icon
    )

    @Test
    fun `destinations pass through unchanged`() {
        val items = persistentListOf<DrawerItem<TestDestination>>(dashboard, settingsItem)

        assertEquals(listOf(dashboard, settingsItem), flattenForNavigationSuite(items))
    }

    @Test
    fun `group is unwrapped to its inner destinations`() {
        val group = DrawerItem.Group(
            label = NativeText.Simple("Scrum"),
            items = listOf(scrumBacklog, scrumClosedSprints)
        )
        val items = persistentListOf<DrawerItem<TestDestination>>(dashboard, group, settingsItem)

        assertEquals(
            listOf(dashboard, scrumBacklog, scrumClosedSprints, settingsItem),
            flattenForNavigationSuite(items)
        )
    }

    @Test
    fun `divider is dropped`() {
        val items = persistentListOf<DrawerItem<TestDestination>>(dashboard, DrawerItem.Divider, settingsItem)

        assertEquals(listOf(dashboard, settingsItem), flattenForNavigationSuite(items))
    }
}
