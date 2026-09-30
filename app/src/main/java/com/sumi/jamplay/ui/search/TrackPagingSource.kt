package com.sumi.jamplay.ui.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.repository.TrackRepository
import kotlinx.coroutines.CancellationException

class TrackPagingSource(
    private val repository: TrackRepository,
    private val query: String
) : PagingSource<Int, Track>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val key = params.key ?: 0
        val offset = if (params is LoadParams.Prepend) (key - params.loadSize).coerceAtLeast(0) else key
        val limit = if (params is LoadParams.Prepend) key - offset else params.loadSize
        return try {
            val page = repository.searchTracks(query, offset, limit)
            LoadResult.Page(
                data = page.tracks,
                prevKey = if (offset == 0 || page.tracks.isEmpty()) null else offset,
                nextKey = page.nextOffset
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey ?: 0
    }
}
