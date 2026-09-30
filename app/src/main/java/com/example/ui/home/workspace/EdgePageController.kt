package com.example.ui.home.workspace

enum class EdgePageAction { NONE, PREVIOUS, NEXT, CREATE_PAGE }

/** Called after a continuous 450 ms edge dwell; it never targets the Life Bento special page. */
object EdgePageController {
    const val DWELL_MS = 450L

    fun action(
        pointerX: Float,
        viewportWidth: Float,
        currentPage: Int,
        workspacePageCount: Int,
        thresholdPx: Float,
        allowCreate: Boolean,
    ): EdgePageAction {
        if (viewportWidth <= 0f || currentPage !in 0 until workspacePageCount)
            return EdgePageAction.NONE
        if (pointerX <= thresholdPx && currentPage > 0) return EdgePageAction.PREVIOUS
        if (pointerX >= viewportWidth - thresholdPx) {
            if (currentPage < workspacePageCount - 1) return EdgePageAction.NEXT
            if (allowCreate) return EdgePageAction.CREATE_PAGE
        }
        return EdgePageAction.NONE
    }
}
