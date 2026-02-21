package com.example.memokeyword.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.example.memokeyword.data.Keyword
import com.example.memokeyword.data.Memo
import com.example.memokeyword.data.MemoPhoto
import com.example.memokeyword.data.MemoWithKeywords
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.util.LinkContentFetcher
import kotlinx.coroutines.launch

class MemoViewModel(private val repository: MemoRepository) : ViewModel() {

    val allMemosWithKeywords: LiveData<List<MemoWithKeywords>> =
        repository.allMemosWithKeywords

    val allKeywords: LiveData<List<Keyword>> = repository.allKeywords

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _searchMode = MutableLiveData<SearchMode>(SearchMode.KEYWORD)
    val searchMode: LiveData<SearchMode> = _searchMode

    val searchResults: LiveData<List<MemoWithKeywords>> =
        _searchQuery.switchMap { query ->
            if (query.isBlank()) {
                MutableLiveData(emptyList())
            } else {
                when (_searchMode.value) {
                    SearchMode.KEYWORD -> repository.searchByKeyword(query)
                    SearchMode.CONTENT -> repository.searchByContent(query)
                    else -> repository.searchByKeyword(query)
                }
            }
        }

    private val _saveResult = MutableLiveData<Long?>()
    val saveResult: LiveData<Long?> = _saveResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _linkFetchResult = MutableLiveData<LinkContentFetcher.LinkContent?>()
    val linkFetchResult: LiveData<LinkContentFetcher.LinkContent?> = _linkFetchResult

    private val _linkFetchLoading = MutableLiveData<Boolean>(false)
    val linkFetchLoading: LiveData<Boolean> = _linkFetchLoading

    // 편집 화면의 첨부 사진 URI 목록 (최대 5개)
    private val _editPhotoUris = MutableLiveData<List<Uri>>(emptyList())
    val editPhotoUris: LiveData<List<Uri>> = _editPhotoUris

    fun addPhotos(uris: List<Uri>) {
        val current = _editPhotoUris.value ?: emptyList()
        val combined = (current + uris).distinct()
        if (combined.size > 5) {
            _errorMessage.value = "사진은 최대 5개까지만 첨부할 수 있습니다."
        }
        _editPhotoUris.value = combined.take(5)
    }

    fun removePhoto(uri: Uri) {
        _editPhotoUris.value = (_editPhotoUris.value ?: emptyList()) - uri
    }

    fun setEditPhotosFromPaths(filePaths: List<String>) {
        _editPhotoUris.value = filePaths.map { Uri.parse("file://$it") }
    }

    fun clearEditPhotos() {
        _editPhotoUris.value = emptyList()
    }

    fun saveMemo(
        context: Context,
        title: String,
        content: String,
        userKeywords: List<String>,
        memoId: Long = 0L
    ) {
        if (title.isBlank() && content.isBlank()) {
            _errorMessage.value = "제목 또는 내용을 입력해주세요."
            return
        }
        val photoUris = _editPhotoUris.value ?: emptyList()
        viewModelScope.launch {
            try {
                val id = repository.saveMemo(context, title, content, userKeywords, photoUris, memoId)
                _saveResult.value = id
            } catch (e: Exception) {
                _errorMessage.value = "저장 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun deleteMemo(memo: Memo) {
        viewModelScope.launch {
            repository.deleteMemo(memo)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        _searchQuery.value = _searchQuery.value
    }

    suspend fun getMemoWithKeywords(memoId: Long): MemoWithKeywords? {
        return repository.getMemoWithKeywords(memoId)
    }

    suspend fun getPhotosForMemo(memoId: Long): List<MemoPhoto> {
        return repository.getPhotosForMemo(memoId)
    }

    suspend fun searchKeywordSuggestions(prefix: String): List<Keyword> {
        return repository.searchKeywordSuggestions(prefix)
    }

    fun fetchLinkContent(url: String) {
        if (!LinkContentFetcher.isValidUrl(url)) {
            _errorMessage.value = "올바른 URL을 입력해주세요. (http:// 또는 https://로 시작)"
            return
        }
        _linkFetchLoading.value = true
        viewModelScope.launch {
            val result = LinkContentFetcher.fetch(url)
            _linkFetchLoading.value = false
            result.fold(
                onSuccess = { _linkFetchResult.value = it },
                onFailure = { _errorMessage.value = "링크를 읽어오는 데 실패했습니다: ${it.message}" }
            )
        }
    }

    fun clearLinkFetchResult() {
        _linkFetchResult.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    enum class SearchMode {
        KEYWORD, CONTENT
    }
}
