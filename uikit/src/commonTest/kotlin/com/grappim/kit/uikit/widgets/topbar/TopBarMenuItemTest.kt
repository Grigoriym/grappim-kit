package com.grappim.kit.uikit.widgets.topbar

import com.grappim.kit.uikit.NativeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopBarMenuItemTest {

    @Test
    fun `selecting an item dismisses the menu before running its action`() {
        val calls = mutableListOf<String>()
        val item = TopBarMenuItem(text = NativeText.Simple("Edit"), onClick = { calls += "onClick" })

        item.select(dismissMenu = { calls += "dismiss" })

        assertEquals(listOf("dismiss", "onClick"), calls)
    }

    @Test
    fun `menu item is enabled by default`() {
        assertTrue(TopBarMenuItem(text = NativeText.Simple("Edit"), onClick = {}).enabled)
    }
}
