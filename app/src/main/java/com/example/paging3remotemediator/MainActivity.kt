package com.example.paging3remotemediator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.paging3remotemediator.ui.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModel = ViewModelProvider(this, Injection.provideViewModelFactory(this, this))[SearchRepositoriesViewModel::class.java]

        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        list.addItemDecoration(decoration)

        bindState(viewModel.state, viewModel.pagingDataFlow, viewModel.accept)
    }

    private fun bindState(uiState: StateFlow<UiState>, pagingData: Flow<PagingData<UiModel>>, uiActions: (UiAction) -> Unit){
        val repoAdapter = ReposAdapter()
        val header = ReposLoadStateAdapter { repoAdapter.retry()}
        list.adapter = repoAdapter.withLoadStateHeaderAndFooter(header, ReposLoadStateAdapter{repoAdapter.retry()})

        bindSearch(uiState, uiActions)

        bindList(header, repoAdapter, uiState, pagingData, uiActions)

    }

    private fun bindSearch(uiState: StateFlow<UiState>, onQueryChanged: (UiAction.Search) -> Unit) {
        search_repo.setOnEditorActionListener {_, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }

        search_repo.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }

        lifecycleScope.launch { uiState.map { it.query }.distinctUntilChanged().collect { search_repo.setText(it) } }
    }

    private fun updateRepoListFromInput(onQueryChanged: (UiAction.Search) -> Unit) {
        search_repo.text.trim().let {
            if (it.isNotEmpty()) {
                list.scrollToPosition(0)
                onQueryChanged(UiAction.Search(it.toString()))
            }
        }
    }

    private fun bindList(
        header: ReposLoadStateAdapter,
        repoAdapter: ReposAdapter,
        uiState: StateFlow<UiState>,
        pagingData: Flow<PagingData<UiModel>>,
        onScrollChanged: (UiAction.Scroll) -> Unit
    ) {
        retry_button.setOnClickListener { repoAdapter.retry() }
        list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) onScrollChanged(UiAction.Scroll(uiState.value.query))
            }
        })

        val notLoading = repoAdapter.loadStateFlow
            .asRemotePresentationState()
            .map { it == RemotePresentationState.PRESENTED }

        val hasNotScrolledFroCurrentSearch = uiState
            .map { it.hasNotScrolledFroCurrentSearch }
            .distinctUntilChanged()

        val shouldScrollToTop = combine(notLoading, hasNotScrolledFroCurrentSearch, Boolean::and).distinctUntilChanged()

        lifecycleScope.launch {
            pagingData.collectLatest(repoAdapter::submitData)
        }

        lifecycleScope.launch {
            shouldScrollToTop.collect { shouldScroll ->
                if (shouldScroll) list.scrollToPosition(0)
            }
        }

        lifecycleScope.launch {
            repoAdapter.loadStateFlow.collect { loadState ->
                header.loadState = loadState.mediator?.refresh?.takeIf { it is LoadState.Error && repoAdapter.itemCount > 0 }?: loadState.prepend

                val isListEmpty = loadState.refresh is LoadState.NotLoading && repoAdapter.itemCount == 0
                emptyList.isVisible = isListEmpty

                list.isVisible = loadState.source.refresh is LoadState.NotLoading || loadState.mediator?.refresh is LoadState.NotLoading

                progress_bar.isVisible = loadState.mediator?.refresh is LoadState.Loading

                retry_button.isVisible = loadState.mediator?.refresh is LoadState.Error && repoAdapter.itemCount == 0

                val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                errorState?.let {
                    Toast.makeText(
                        this@MainActivity,
                        "\uD83D\uDE28 Wooops ${it.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}