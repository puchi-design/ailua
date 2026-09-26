package com.example.ui.home

/**
 * Pure ordering helpers for the Home App Grid.
 *
 * The saved order is a list of stable app ids. It is normalized against the ids
 * that currently exist in the app catalog:
 * - ids that still exist are kept in saved order
 * - unknown / removed ids are dropped
 * - brand new apps are appended at the end in catalog order
 *
 * Empty or missing storage simply yields the catalog default order.
 */
object HomeAppOrder {

    fun normalize(savedIds: List<String>, availableIds: List<String>): List<String> {
        val available = availableIds.distinct()
        val kept = savedIds.distinct().filter { it in available }
        val missing = available.filter { it !in kept }
        return kept + missing
    }

    fun move(ids: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (fromIndex == toIndex) return ids
        if (fromIndex !in ids.indices || toIndex !in ids.indices) return ids
        val mutable = ids.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return mutable.toList()
    }
}
