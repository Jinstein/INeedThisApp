package com.example.memokeyword.ui

import android.os.Bundle
import android.view.*
import android.widget.RadioGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memokeyword.data.AppDatabase
import com.example.memokeyword.databinding.FragmentSearchBinding
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.ui.adapter.MemoAdapter
import com.example.memokeyword.viewmodel.MemoViewModel
import com.example.memokeyword.viewmodel.MemoViewModelFactory

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemoViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        MemoViewModelFactory(MemoRepository(db.memoDao(), db.keywordDao()))
    }

    private lateinit var memoAdapter: MemoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchInput()
        setupSearchModeToggle()
        observeSearchResults()
    }

    private fun setupRecyclerView() {
        memoAdapter = MemoAdapter(
            onMemoClick = { memoWithKeywords ->
                val action = SearchFragmentDirections
                    .actionSearchToMemoEdit(memoWithKeywords.memo.id)
                findNavController().navigate(action)
            },
            onMemoLongClick = { false }
        )
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memoAdapter
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text.toString().trim()
            viewModel.setSearchQuery(query)

            if (query.isBlank()) {
                binding.tvResultCount.text = ""
                binding.tvNoResult.visibility = View.GONE
            }
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text?.clear()
            viewModel.setSearchQuery("")
        }
    }

    private fun setupSearchModeToggle() {
        binding.rgSearchMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.rbKeyword.id -> MemoViewModel.SearchMode.KEYWORD
                binding.rbContent.id -> MemoViewModel.SearchMode.CONTENT
                else -> MemoViewModel.SearchMode.KEYWORD
            }
            viewModel.setSearchMode(mode)
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            memoAdapter.submitList(results)

            val query = binding.etSearch.text.toString().trim()
            if (query.isNotBlank()) {
                binding.tvResultCount.text = "검색 결과: ${results.size}개"
                binding.tvNoResult.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
