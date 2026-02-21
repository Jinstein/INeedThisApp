package com.example.memokeyword.repository

import androidx.lifecycle.LiveData
import com.example.memokeyword.data.*
import com.example.memokeyword.util.KeywordExtractor

class MemoRepository(
    private val memoDao: MemoDao,
    private val keywordDao: KeywordDao
) {

    val allMemosWithKeywords: LiveData<List<MemoWithKeywords>> =
        memoDao.getAllMemosWithKeywords()

    val allKeywords: LiveData<List<Keyword>> = keywordDao.getAllKeywords()

    suspend fun saveMemo(
        title: String,
        content: String,
        userKeywords: List<String>,
        memoId: Long = 0
    ): Long {
        val now = System.currentTimeMillis()

        // 자동 추출 키워드 + 사용자 입력 키워드 합치기
        val autoKeywords = KeywordExtractor.extract("$title $content")
        val allKeywords = (userKeywords + autoKeywords).distinct()

        val memo = if (memoId == 0L) {
            Memo(title = title, content = content, createdAt = now, updatedAt = now)
        } else {
            Memo(id = memoId, title = title, content = content,
                createdAt = now, updatedAt = now)
        }

        val savedMemoId = if (memoId == 0L) {
            memoDao.insertMemo(memo)
        } else {
            memoDao.updateMemo(memo)
            // 기존 키워드 연결 삭제 후 재연결
            memoDao.deleteKeywordRefsForMemo(memoId)
            memoId
        }

        // 키워드 저장 및 연결
        for (word in allKeywords) {
            if (word.isBlank()) continue
            val existing = keywordDao.getKeywordByWord(word)
            val keywordId = existing?.id ?: keywordDao.insertKeyword(Keyword(word = word))
            if (keywordId > 0) {
                memoDao.insertKeywordRef(MemoKeywordCrossRef(savedMemoId, keywordId))
            }
        }

        // 고아 키워드 정리
        keywordDao.deleteOrphanKeywords()

        return savedMemoId
    }

    suspend fun deleteMemo(memo: Memo) {
        memoDao.deleteMemo(memo)
        keywordDao.deleteOrphanKeywords()
    }

    suspend fun getMemoWithKeywords(memoId: Long): MemoWithKeywords? {
        return memoDao.getMemoWithKeywords(memoId)
    }

    fun searchByKeyword(keyword: String): LiveData<List<MemoWithKeywords>> {
        return memoDao.searchMemosByKeyword(keyword)
    }

    fun searchByContent(query: String): LiveData<List<MemoWithKeywords>> {
        return memoDao.searchMemosByContent(query)
    }

    suspend fun searchKeywordSuggestions(prefix: String): List<Keyword> {
        return keywordDao.searchKeywords(prefix)
    }
}
