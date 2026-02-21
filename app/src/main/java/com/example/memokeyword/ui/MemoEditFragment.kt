package com.example.memokeyword.ui

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memokeyword.data.AppDatabase
import com.example.memokeyword.databinding.FragmentMemoEditBinding
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.ui.adapter.KeywordChipAdapter
import com.example.memokeyword.ui.adapter.PhotoThumbnailAdapter
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
        MemoViewModelFactory(MemoRepository(db.memoDao(), db.keywordDao(), db.memoPhotoDao()))
    }

    private lateinit var keywordAdapter: KeywordChipAdapter
    private lateinit var photoAdapter: PhotoThumbnailAdapter
    private var existingMemoId: Long = 0L

    private val pickPhotos = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val current = viewModel.editPhotoUris.value ?: emptyList()
            val remaining = 5 - current.size
            if (remaining <= 0) {
                Toast.makeText(requireContext(), "사진은 최대 5개까지만 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            viewModel.addPhotos(uris.take(remaining))
        }
    }

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
        setupPhotoRecyclerView()
        setupKeywordInput()
        setupAutoExtract()
        setupSaveButton()
        setupLinkFetch()
        setupPhotoButton()
        observeViewModel()

        if (existingMemoId != 0L) {
            loadExistingMemo()
        } else {
            viewModel.clearEditPhotos()
            // 스크린샷 감지로 전달된 URI가 있으면 자동 첨부
            val pendingUri = viewModel.pendingScreenshotUri.value
            if (pendingUri != null) {
                viewModel.addPhotos(listOf(pendingUri))
                viewModel.clearPendingScreenshot()
            }
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

    private fun setupPhotoRecyclerView() {
        photoAdapter = PhotoThumbnailAdapter(
            onRemoveClick = { uri -> viewModel.removePhoto(uri) }
        )
        binding.rvPhotos.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = photoAdapter
        }
    }

    private fun setupPhotoButton() {
        binding.btnAddPhoto.setOnClickListener {
            val current = viewModel.editPhotoUris.value ?: emptyList()
            if (current.size >= 5) {
                Toast.makeText(requireContext(), "사진은 최대 5개까지만 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickPhotos.launch("image/*")
        }
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

    private fun setupLinkFetch() {
        binding.btnFetchLink.setOnClickListener {
            val url = binding.etLinkUrl.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(requireContext(), "URL을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.fetchLinkContent(url)
        }

        binding.etLinkUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val url = binding.etLinkUrl.text.toString().trim()
                if (url.isNotBlank()) viewModel.fetchLinkContent(url)
                true
            } else false
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()
            val userKeywords = keywordAdapter.getKeywords()

            viewModel.saveMemo(requireContext(), title, content, userKeywords, existingMemoId)
        }
    }

    private fun observeViewModel() {
        viewModel.saveResult.observe(viewLifecycleOwner) { id ->
            if (id != null) {
                Toast.makeText(requireContext(), "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                viewModel.clearSaveResult()
                viewModel.clearEditPhotos()
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.linkFetchLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressLinkFetch.isVisible = isLoading
            binding.btnFetchLink.isEnabled = !isLoading
        }

        viewModel.linkFetchResult.observe(viewLifecycleOwner) { linkContent ->
            if (linkContent != null) {
                if (binding.etTitle.text.isNullOrBlank() && linkContent.title.isNotBlank()) {
                    binding.etTitle.setText(linkContent.title)
                }
                val currentContent = binding.etContent.text.toString()
                val separator = if (currentContent.isNotBlank()) "\n\n" else ""
                val appendedContent = "$currentContent$separator${linkContent.content}"
                binding.etContent.setText(appendedContent)
                binding.etContent.setSelection(appendedContent.length)
                binding.etLinkUrl.text?.clear()
                Toast.makeText(requireContext(), "링크 내용을 가져왔습니다.", Toast.LENGTH_SHORT).show()
                viewModel.clearLinkFetchResult()
            }
        }

        viewModel.editPhotoUris.observe(viewLifecycleOwner) { uris ->
            photoAdapter.setPhotos(uris)
            val count = uris.size
            binding.btnAddPhoto.isEnabled = count < 5
            binding.btnAddPhoto.text = if (count > 0) "+ 사진 추가 ($count/5)" else "+ 사진 추가"
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

                // 기존 사진 불러오기
                val photos = viewModel.getPhotosForMemo(existingMemoId)
                viewModel.setEditPhotosFromPaths(photos.map { it.filePath })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
