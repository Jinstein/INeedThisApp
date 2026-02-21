package com.example.memokeyword.ui

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.memokeyword.data.AppDatabase
import com.example.memokeyword.databinding.FragmentMemoEditBinding
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.ui.adapter.KeywordChipAdapter
import com.example.memokeyword.util.KeywordExtractor
import com.example.memokeyword.viewmodel.MemoViewModel
import com.example.memokeyword.viewmodel.MemoViewModelFactory
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.launch

class MemoEditFragment : Fragment() {

    private var _binding: FragmentMemoEditBinding? = null
    private val binding get() = _binding!!

    private val args: MemoEditFragmentArgs by navArgs()

    private val viewModel: MemoViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        MemoViewModelFactory(MemoRepository(db.memoDao(), db.keywordDao()))
    }

    private lateinit var keywordAdapter: KeywordChipAdapter
    private var existingMemoId: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingMemoId = args.memoId
        setupKeywordRecyclerView()
        setupKeywordInput()
        setupAutoExtract()
        setupSaveButton()
        observeViewModel()

        if (existingMemoId != 0L) {
            loadExistingMemo()
        }
    }

    private fun setupKeywordRecyclerView() {
        keywordAdapter = KeywordChipAdapter(
            onRemoveClick = { keyword -> keywordAdapter.removeKeyword(keyword) }
        )
        val flexboxLayoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }
        binding.rvKeywords.apply {
            layoutManager = flexboxLayoutManager
            adapter = keywordAdapter
        }
        keywordAdapter.setKeywords(emptyList(), showRemove = true)
    }

    private fun setupKeywordInput() {
        binding.etKeywordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addKeywordFromInput()
                true
            } else false
        }
        binding.btnAddKeyword.setOnClickListener { addKeywordFromInput() }
    }

    private fun addKeywordFromInput() {
        val input = binding.etKeywordInput.text.toString().trim()
        if (input.isNotBlank()) {
            val parsed = KeywordExtractor.parseUserKeywords(input)
            parsed.forEach { keywordAdapter.addKeyword(it) }
            binding.etKeywordInput.text?.clear()
        }
    }

    private fun setupAutoExtract() {
        // 내용 변경 시 키워드 미리보기
        binding.etContent.doAfterTextChanged { text ->
            val content = text.toString()
            val title = binding.etTitle.text.toString()
            if (content.length > 10) {
                val preview = KeywordExtractor.extract("$title $content")
                binding.tvAutoKeywordPreview.text = "자동 추출: " + preview.joinToString(", ")
            } else {
                binding.tvAutoKeywordPreview.text = ""
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()
            val userKeywords = keywordAdapter.getKeywords()

            viewModel.saveMemo(title, content, userKeywords, existingMemoId)
        }
    }

    private fun observeViewModel() {
        viewModel.saveResult.observe(viewLifecycleOwner) { id ->
            if (id != null) {
                Toast.makeText(requireContext(), "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                viewModel.clearSaveResult()
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun loadExistingMemo() {
        lifecycleScope.launch {
            val memoWithKeywords = viewModel.getMemoWithKeywords(existingMemoId)
            memoWithKeywords?.let { mwk ->
                binding.etTitle.setText(mwk.memo.title)
                binding.etContent.setText(mwk.memo.content)
                val words = mwk.keywords.map { it.word }
                keywordAdapter.setKeywords(words, showRemove = true)
                binding.btnSave.text = "수정 완료"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
