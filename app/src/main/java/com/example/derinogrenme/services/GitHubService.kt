package com.example.derinogrenme.services

import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("repos/ProjectDepo/derin-ogrenme-images/contents/{path}")
    suspend fun getContents(@Path("path") path: String): List<GitHubContent>
}

data class GitHubContent(
    val name: String,
    val path: String,
    val type: String,
    val download_url: String?
) 