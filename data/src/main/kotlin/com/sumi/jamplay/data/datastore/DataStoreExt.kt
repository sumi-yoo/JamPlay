package com.sumi.jamplay.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Context 확장으로 DataStore 싱글톤
val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_prefs")
val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_prefs")