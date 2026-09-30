package com.example.ui.design.launcher.layout

import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.GridSpec
import kotlin.math.abs

enum class DragDirection(val dx: Int, val dy: Int) {
    RIGHT(1, 0), LEFT(-1, 0), DOWN(0, 1), UP(0, -1)
}
data class LayoutSolution(val placements: Map<String, CellRect>, val cost: Int)

/** Computes a preview without mutating a model or touching storage. */
object ReorderSolver {
    fun solve(items: List<DesktopItem>, dragged: DesktopItem, targetX: Int, targetY: Int,
              grid: GridSpec = GridSpec(), direction: DragDirection = DragDirection.RIGHT): LayoutSolution? {
        val target = CellRect(targetX, targetY, dragged.spanX, dragged.spanY)
        val bounds = GridOccupancy(grid.columns, grid.rows)
        if (dragged.locked || !bounds.contains(target)) return null
        val others = items.filterNot { it.id == dragged.id }
        val initial = others.associate { it.id to CellRect(it.cellX, it.cellY, it.spanX, it.spanY) }
        val occupied = GridOccupancy.from(grid, others)
        if (occupied.isVacant(targetX, targetY, target.spanX, target.spanY))
            return LayoutSolution(initial + (dragged.id to target), 0)

        val shifted = initial.toMutableMap()
        val locked = others.filter { it.locked }.map { it.id }.toSet()
        val visiting = mutableSetOf<String>()
        fun push(id: String): Boolean {
            if (id in locked || !visiting.add(id)) return false
            val old = shifted.getValue(id)
            val next = old.copy(x = old.x + direction.dx, y = old.y + direction.dy)
            if (!bounds.contains(next) || next.overlaps(target)) { visiting.remove(id); return false }
            val blockers = shifted.filter { (other, rect) -> other != id && rect.overlaps(next) }.keys.toList()
            if (blockers.any { !push(it) }) { visiting.remove(id); return false }
            shifted[id] = next
            visiting.remove(id)
            return true
        }
        val conflicts = shifted.filter { (_, rect) -> rect.overlaps(target) }.keys.toList()
        if (conflicts.all { push(it) } && shifted.values.none { it.overlaps(target) }) {
            val moved = shifted.count { (id, rect) -> initial[id] != rect }
            val distance = shifted.entries.sumOf { (id, rect) ->
                val old = initial.getValue(id)
                abs(rect.x - old.x) + abs(rect.y - old.y)
            }
            return LayoutSolution(shifted + (dragged.id to target), moved * 100 + distance * 10)
        }
        val nearest = (0 until grid.rows).flatMap { y ->
            (0 until grid.columns).map { x -> CellRect(x, y, target.spanX, target.spanY) }
        }.filter { occupied.isVacant(it.x, it.y, it.spanX, it.spanY) &&
            (it.x != dragged.cellX || it.y != dragged.cellY) }
            .minWithOrNull(compareBy<CellRect> { abs(it.x - targetX) + abs(it.y - targetY) }
                .thenBy { if ((it.x - targetX) * direction.dx + (it.y - targetY) * direction.dy >= 0) 0 else 1 }
                .thenBy { it.y }.thenBy { it.x }) ?: return null
        return LayoutSolution(initial + (dragged.id to nearest),
            (abs(nearest.x - targetX) + abs(nearest.y - targetY)) * 10 + 1)
    }
}
