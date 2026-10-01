package com.sumi.jamplay.ui.search

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.FragmentSearchBinding
import com.sumi.jamplay.ui.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {
    private val viewModel: SearchViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentSearchBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = FragmentSearchBinding.bind(view)
        binding = content
        val adapter = TrackPagingAdapter { track ->
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val tracks = (content.results.adapter as? TrackPagingAdapter)?.snapshot()?.items.orEmpty()
                playerViewModel.play(track, tracks)
                findNavController().navigate(R.id.player)
            }
        }
        content.results.layoutManager = LinearLayoutManager(requireContext())
        content.results.adapter = adapter
        content.results.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, child: View, parent: RecyclerView, state: RecyclerView.State) {
                // 항목 사이에만 간격을 주고 목록 아래에는 여백을 추가하지 않는다.
                if (parent.getChildAdapterPosition(child) > 0) outRect.top = (8 * resources.displayMetrics.density).toInt()
            }
        })
        val historyAdapter = SearchHistoryAdapter { keyword ->
            content.searchInput.setText(keyword)
            content.searchInput.setSelection(keyword.length)
            submitSearch()
        }
        content.history.layoutManager = LinearLayoutManager(requireContext())
        content.history.adapter = historyAdapter
        content.searchInput.setText(viewModel.text.value)
        content.searchInput.setSelection(content.searchInput.length())
        content.searchInput.doAfterTextChanged { viewModel.onTextChanged(it?.toString().orEmpty()) }
        content.searchInput.setOnFocusChangeListener { _, focused -> viewModel.setSearching(focused) }
        content.searchInput.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else false
        }
        val back = object : OnBackPressedCallback(viewModel.isSearching.value) {
            override fun handleOnBackPressed() = closeSearchInput()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, back)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tracks.collectLatest { adapter.submitData(it) } }
                launch { viewModel.recentSearches.collect { historyAdapter.submitList(it) } }
                launch {
                    combine(playerViewModel.currentTrack, playerViewModel.isPlaying) { track, playing -> track?.id to playing }
                        .collect { (id, playing) -> adapter.updatePlayback(id, playing) }
                }
                launch {
                    combine(viewModel.isSearching, adapter.loadStateFlow) { searching, state -> searching to state }
                        .collect { (searching, state) ->
                            back.isEnabled = searching
                            content.history.isVisible = searching
                            content.results.isVisible = !searching
                            val loading = state.refresh is LoadState.Loading || state.append is LoadState.Loading
                            val failed = state.refresh is LoadState.Error || state.append is LoadState.Error || state.prepend is LoadState.Error
                            content.loading.isVisible = !searching && loading
                            content.errorPanel.isVisible = !searching && failed
                            content.emptyMessage.isVisible = !searching && !loading && !failed && adapter.itemCount == 0
                        }
                }
            }
        }
    }

    private fun submitSearch() {
        viewModel.search()
        closeSearchInput()
    }

    private fun closeSearchInput() {
        val content = binding ?: return
        content.searchInput.clearFocus()
        content.root.requestFocus()
        viewModel.setSearching(false)
        WindowCompat.getInsetsController(requireActivity().window, content.root).hide(WindowInsetsCompat.Type.ime())
    }

    override fun onDestroyView() {
        binding?.results?.adapter = null
        binding?.history?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
