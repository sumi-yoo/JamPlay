package com.sumi.jamplay.data.api

import com.sumi.jamplay.data.model.JamendoTrackResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApi {
    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,          // 페이지 계산용 offset
        @Query("search") query: String? = null,    // 검색어
        @Query("audioformat") audioFormat: String = "mp31",
        @Query("license") license: String = "by,by-sa"
    ): JamendoTrackResponse
}