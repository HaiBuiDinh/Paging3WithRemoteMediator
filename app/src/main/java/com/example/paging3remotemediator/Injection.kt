package com.example.paging3remotemediator

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.example.paging3remotemediator.api.GithubService
import com.example.paging3remotemediator.data.GithubRepository
import com.example.paging3remotemediator.db.RepoDatabase
import com.example.paging3remotemediator.ui.ViewModelFactory

object Injection {

    private fun provideGithubRepository(context: Context): GithubRepository {
        return GithubRepository(GithubService.create(), RepoDatabase.getInstance(context))
    }

    fun provideViewModelFactory(context: Context, owner: SavedStateRegistryOwner): ViewModelProvider.Factory {
        return ViewModelFactory(owner, provideGithubRepository(context))
    }
}