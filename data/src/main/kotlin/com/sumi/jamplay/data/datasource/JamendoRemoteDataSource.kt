package com.sumi.jamplay.data.datasource

import com.sumi.jamplay.data.api.JamendoApi
import com.sumi.jamplay.data.model.JamendoTrackResponse
import java.io.IOException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Named

class JamendoRemoteDataSource @Inject constructor(
    private val api: JamendoApi,
    @Named("JamendoClientId") private val clientId: String
) {

    suspend fun searchTracks(query: String, offset: Int, limit: Int): JamendoTrackResponse {
        var attempt = 0
        while (true) {
            val response = api.searchTracks(
                clientId = clientId,
                query = query,
                offset = offset,
                limit = limit,
                audioFormat = "mp31",
                license = "by,by-sa"
            )
            if (response.headers["status"] != "success") {
                throw IOException("Jamendo search failed (code=${response.headers["code"]})")
            }

            attempt++
            if (response.results.isNotEmpty() || attempt >= MAX_ATTEMPTS) return response

            // Jamendo가 일시적으로 성공 상태의 빈 목록을 반환하므로 같은 구간을 재확인한다.
            // 끝까지 비어 있으면 정상적인 검색 결과 없음 또는 마지막 페이지로 처리한다.
            delay(EMPTY_RESPONSE_RETRY_DELAY_MS)
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val EMPTY_RESPONSE_RETRY_DELAY_MS = 200L
    }
}