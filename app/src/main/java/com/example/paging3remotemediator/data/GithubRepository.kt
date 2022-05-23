package com.example.paging3remotemediator.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.paging3remotemediator.api.GithubService
import com.example.paging3remotemediator.db.RepoDatabase
import com.example.paging3remotemediator.model.Repo
import kotlinx.coroutines.flow.Flow

class GithubRepository(
    private val service: GithubService,
    private val database: RepoDatabase
    ) {

    fun getSearchResultStream(query: String): Flow<PagingData<Repo>> {
        Log.d("GithubRepository", "New query: $query")

        // appending '%' so we can allow other characters to be before and after the query string
        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { database.reposDao().reposByName(dbQuery) }

        @OptIn(ExperimentalPagingApi::class)
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = GithubRemoteMediator(
                query,
                service,
                database
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }


    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}