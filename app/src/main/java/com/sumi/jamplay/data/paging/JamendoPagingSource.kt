package com.sumi.jamplay.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.data.datasource.JamendoRemoteDataSource

class JamendoPagingSource(
    private val remoteDataSource: JamendoRemoteDataSource,
    private val query: String
) : PagingSource<Int, Track>() {

    companion object {
        private const val MAX_RETRY = 5
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val page = params.key ?: 1  // 첫 페이지 1부터 시작
        val perPage = params.loadSize
        // offset 계산: 첫 페이지 0, 두 번째 페이지 60, 세 번째 페이지 90, ...
        val offset = if (page == 1) 0 else 60 + (page - 1) * 30
        var attempt = 0
        var tracks: List<Track> = emptyList()

        while (attempt < MAX_RETRY) {
            try {
                val response = remoteDataSource.searchTracks(query, offset, perPage)
                tracks = response.results.map {
                    Track(
                        id = it.id,
                        name = it.name,
                        artistName = it.artistName,
                        albumName = it.albumName,
                        artworkUrl = it.artworkUrl,
                        streamUrl = it.audioUrl
                    )
                }

                if (tracks.isNotEmpty()) {
                    return LoadResult.Page(
                        data = tracks,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (tracks.isEmpty()) null else page + 1
                    )
                } else {
                    attempt++
                }

            } catch (e: Exception) {
                return LoadResult.Page(
                    data = tracks,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (tracks.isEmpty()) null else page + 1
                )
            }
        }

        // 결과가 없어도 빈 리스트로 Page 반환
        return LoadResult.Page(
            data = tracks,
            prevKey = if (page == 1) null else page - 1,
            nextKey = if (tracks.isEmpty()) null else page + 1
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}