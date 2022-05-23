package com.example.paging3remotemediator.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.paging3remotemediator.data.GithubRepository
import com.example.paging3remotemediator.model.Repo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchRepositoriesViewModel(
    private val repository: GithubRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val state: StateFlow<UiState>

    val pagingDataFlow: Flow<PagingData<UiModel>>

    val accept: (UiAction) -> Unit

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction>()
        val searches = actionStateFlow
            .filterIsInstance<UiAction.Search>()
            .distinctUntilChanged() //lọc tất cả các giá trị trùng nhau khỏi fow đang xét
            .onStart { emit(UiAction.Search(query = initialQuery)) } //có thể hiểu đơn giản onStart thêm cái kia vào đầu của flow trước khi n emit các giá trị khác
        val queryScrolled = actionStateFlow
            .filterIsInstance<UiAction.Scroll>()
            .distinctUntilChanged()
                //flow là dòng dữ liệu lạnh, cái này giúp chuyển từ cold về hot giữ n luôn đực cập nhật
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
            .onStart { emit(UiAction.Scroll(lastQueryScrolled)) }

        pagingDataFlow = searches
            .flatMapLatest { it -> searchRepo(queryString = it.query) } //lấy ra giá trị cuối cùng được emit rồi thực hiện searchRepo()
            .cachedIn(viewModelScope)

        state = combine(searches, queryScrolled, ::Pair)
            .map { (search, scroll) -> UiState(search.query, scroll.currentQuery, search.query != scroll.currentQuery)}
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

        accept = {action -> viewModelScope.launch { actionStateFlow.emit(action) }}
    }

    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state.value.lastQueryScrolled
        super.onCleared()
    }

    private fun searchRepo(queryString: String): Flow<PagingData<UiModel>> =
        repository.getSearchResultStream(queryString)
            .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
            .map {
                it.insertSeparators { before: UiModel.RepoItem?, after: UiModel.RepoItem? ->
                    if (after == null) {
                        return@insertSeparators null
                    }

                    if (before == null) {
                        return@insertSeparators UiModel.SeparatorItem("${after.roundedStartCount}0.000+ stars")
                    }

                    if (before.roundedStartCount > after.roundedStartCount) {
                        if (after.roundedStartCount >= 1) {
                            UiModel.SeparatorItem("${after.roundedStartCount}0.000+ stars")
                        } else {
                            UiModel.SeparatorItem("< 10.000+ start")
                        }
                    } else {
                        null
                    }
                }
            }
}

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(val currentQuery: String) : UiAction()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledFroCurrentSearch: Boolean = false
)

sealed class UiModel{
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

private val UiModel.RepoItem.roundedStartCount: Int
    get() = this.repo.stars / 10_000

private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"
private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val DEFAULT_QUERY = "Android"