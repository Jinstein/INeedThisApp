package com.example.memokeyword.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memokeyword.R
import com.example.memokeyword.data.AppDatabase
import com.example.memokeyword.databinding.FragmentMemoListBinding
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.ui.adapter.MemoAdapter
import com.example.memokeyword.viewmodel.MemoViewModel
import com.example.memokeyword.viewmodel.MemoViewModelFactory

class MemoListFragment : Fragment() {

    private var _binding: FragmentMemoListBinding? = null
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
        _binding = FragmentMemoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        memoAdapter = MemoAdapter(
            onMemoClick = { memoWithKeywords ->
                val action = MemoListFragmentDirections
                    .actionMemoListToMemoEdit(memoWithKeywords.memo.id)
                findNavController().navigate(action)
            },
            onMemoLongClick = { memoWithKeywords ->
                showDeleteDialog(memoWithKeywords.memo.title) {
                    viewModel.deleteMemo(memoWithKeywords.memo)
                }
                true
            }
        )
        binding.rvMemos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memoAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewMemo.setOnClickListener {
            val action = MemoListFragmentDirections.actionMemoListToMemoEdit(0L)
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewModel.allMemosWithKeywords.observe(viewLifecycleOwner) { memos ->
            memoAdapter.submitList(memos)
            binding.tvEmpty.visibility = if (memos.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteDialog(title: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("메모 삭제")
            .setMessage("\"${title.ifBlank { "(제목 없음)" }}\" 메모를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> onConfirm() }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
