package com.sumi.jamplay.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sumi.jamplay.domain.policy.SearchHistoryPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val SEARCH_KEY = stringPreferencesKey("recent_searches")
    private val json = Json { encodeDefaults = true }

    private val dataStore = context.searchDataStore

    // 최근 검색어 조회 (순서 그대로)
    val recentSearches: Flow<List<String>> = dataStore.data
        .map { prefs ->
            prefs[SEARCH_KEY]?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
        }

    // 검색어 추가
    suspend fun addSearch(keyword: String) {
        dataStore.edit { prefs ->
            val current = prefs[SEARCH_KEY]?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
            prefs[SEARCH_KEY] = json.encodeToString(SearchHistoryPolicy.add(current, keyword))
        }
    }
}