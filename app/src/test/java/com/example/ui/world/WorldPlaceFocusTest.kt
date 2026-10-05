package com.example.ui.world

import com.example.data.model.VirtualPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorldPlaceFocusTest {
    private val places = listOf(
        VirtualPlace("street", "青石街23号", "", "街区", ""),
        VirtualPlace("tea", "木兰茶馆", "", "茶馆", ""),
    )

    @Test fun locationCardFocusesTheNamedVirtualPlace() {
        assertEquals("street", matchWorldPlace(places, "青石街23号")?.id)
        assertEquals("tea", matchWorldPlace(places, "木兰茶馆附近")?.id)
        assertEquals("tea", matchWorldPlace(places, "tea")?.id)
    }

    @Test fun unknownLocationDoesNotFocusAnUnrelatedPlace() {
        assertNull(matchWorldPlace(places, "现实世界中的陌生地址"))
        assertEquals("street", matchWorldPlace(places, null)?.id)
    }
}
