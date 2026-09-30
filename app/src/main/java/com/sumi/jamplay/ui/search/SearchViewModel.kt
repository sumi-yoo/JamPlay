package com.sumi.jamplay.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.repository.SearchHistoryRepository
import com.sumi.jamplay.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewViewModel @Inject constructor(
    private val repository: TrackRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    // 입력 중인 텍스트 상태
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 검색 실행 이벤트 (검색 버튼 눌렀을 때만 값 emit)
    private val _query = MutableSharedFlow<String>(replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tracks: Flow<PagingData<Track>> = _query
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(pageSize = 30, prefetchDistance = 10, initialLoadSize = 60),
                pagingSourceFactory = { TrackPagingSource(repository, query) }
            ).flow
        }
        .cachedIn(viewModelScope)

    val recentSearches: StateFlow<List<String>> =
        searchHistoryRepository.recentSearches.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    var acceptsClicks by mutableStateOf(true)
        private set

    init {
        // 초기 화면에서 "" 검색
        viewModelScope.launch {
            _query.emit("")
        }
    }

    fun onTextChanged(newText: String) {
        _text.value = newText
    }

    fun search() {
        viewModelScope.launch {
            _query.emit(_text.value)
            if (_text.value.isNotEmpty()) searchHistoryRepository.addSearch(_text.value)
        }
    }

    fun setSearching(value: Boolean) {
        if (_isSearching.value != value) _isSearching.value = value
    }

    fun enableClicks() { acceptsClicks = true }

    fun disableClicks() { acceptsClicks = false }
}
