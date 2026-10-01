package com.sumi.jamplay.domain.policy

object SearchHistoryPolicy {
    private const val MAX_SIZE = 10

    fun add(current: List<String>, keyword: String): List<String> {
        if (keyword.isEmpty()) return current
        return (listOf(keyword) + current.filterNot { it == keyword }).take(MAX_SIZE)
    }
}
