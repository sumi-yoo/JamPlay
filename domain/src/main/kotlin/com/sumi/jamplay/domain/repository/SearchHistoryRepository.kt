package com.sumi.jamplay.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    val recentSearches: Flow<List<String>>

    suspend fun addSearch(keyword: String)
}
