package com.example.memokeyword.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface KeywordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeyword(keyword: Keyword): Long

    @Query("SELECT * FROM keywords WHERE word = :word LIMIT 1")
    suspend fun getKeywordByWord(word: String): Keyword?

    @Query("SELECT * FROM keywords ORDER BY word ASC")
    fun getAllKeywords(): LiveData<List<Keyword>>

    // 특정 메모에 연결된 키워드 조회
    @Query("""
        SELECT k.* FROM keywords k
        INNER JOIN memo_keyword_cross_ref mkr ON k.id = mkr.keywordId
        WHERE mkr.memoId = :memoId
    """)
    suspend fun getKeywordsForMemo(memoId: Long): List<Keyword>

    // 키워드 검색 자동완성용
    @Query("SELECT * FROM keywords WHERE word LIKE :prefix || '%' ORDER BY word ASC LIMIT 10")
    suspend fun searchKeywords(prefix: String): List<Keyword>

    @Query("DELETE FROM keywords WHERE id NOT IN (SELECT DISTINCT keywordId FROM memo_keyword_cross_ref)")
    suspend fun deleteOrphanKeywords()
}
