package com.example.memokeyword.viewmodel

import androidx.lifecycle.*
import com.example.memokeyword.data.Keyword
import com.example.memokeyword.data.Memo
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

    fun saveMemo(
        title: String,
        content: String,
        userKeywords: List<String>,
        memoId: Long = 0L
    ) {
        if (title.isBlank() && content.isBlank()) {
            _errorMessage.value = "제목 또는 내용을 입력해주세요."
            return
        }
        viewModelScope.launch {
            try {
                val id = repository.saveMemo(title, content, userKeywords, memoId)
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
        // 모드 변경 시 검색 재실행
        _searchQuery.value = _searchQuery.value
    }

    suspend fun getMemoWithKeywords(memoId: Long): MemoWithKeywords? {
        return repository.getMemoWithKeywords(memoId)
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
