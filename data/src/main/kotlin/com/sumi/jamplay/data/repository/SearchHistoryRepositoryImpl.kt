package com.sumi.jamplay.data.repository

import com.sumi.jamplay.data.datastore.SearchPreferencesDataStore
import com.sumi.jamplay.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchDataStore: SearchPreferencesDataStore
) : SearchHistoryRepository {
    override val recentSearches: Flow<List<String>> = searchDataStore.recentSearches

    override suspend fun addSearch(keyword: String) {
        searchDataStore.addSearch(keyword)
    }
}
