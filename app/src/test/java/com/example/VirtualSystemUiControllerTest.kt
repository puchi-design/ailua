package com.example

import com.example.ui.systemui.SystemUiSurface
import com.example.ui.systemui.VirtualSystemUiController
import org.junit.Assert.*
import org.junit.Test

class VirtualSystemUiControllerTest {
    @Test fun coldStartRunsOnceAndDoesNotInterruptOnboardingOrActivityRecreation() {
        val firstInstall = VirtualSystemUiController()
        firstInstall.initializeColdStart(onboardingComplete = false, lockOnColdStart = true)
        firstInstall.initializeColdStart(onboardingComplete = true, lockOnColdStart = true)
        assertFalse(firstInstall.state.value.isLocked)

        val returning = VirtualSystemUiController()
        returning.initializeColdStart(onboardingComplete = true, lockOnColdStart = true)
        assertTrue(returning.state.value.isLocked)
        returning.unlock()
        returning.initializeColdStart(onboardingComplete = true, lockOnColdStart = true)
        assertFalse(returning.state.value.isLocked)

        val disabled = VirtualSystemUiController()
        disabled.initializeColdStart(onboardingComplete = true, lockOnColdStart = false)
        assertFalse(disabled.state.value.isLocked)
    }

    @Test fun panelsKeepLockAndCloseBackToLockscreenWithoutTouchingAppNavigation() {
        val controller = VirtualSystemUiController()
        controller.lock()
        controller.updatePriorityCall("incoming", incoming = true)
        controller.updatePriorityCall("incoming", incoming = false)
        assertTrue(controller.state.value.isLocked)
        assertEquals("incoming", controller.priorityCallId.value)
        controller.lock()
        assertNull(controller.priorityCallId.value)
        controller.openNotifications()
        assertTrue(controller.state.value.isLocked)
        assertEquals(SystemUiSurface.NOTIFICATION_SHADE, controller.state.value.surface)
        controller.openControlCenter()
        assertEquals(0f, controller.state.value.shadeExpansion)
        assertEquals(1f, controller.state.value.controlExpansion)
        controller.closeSystemSurface()
        assertEquals(SystemUiSurface.LOCKSCREEN, controller.state.value.surface)
        controller.unlock()
        controller.toggleNotifications()
        controller.toggleNotifications()
        assertEquals(SystemUiSurface.NONE, controller.state.value.surface)
    }
}
