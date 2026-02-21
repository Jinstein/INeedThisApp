package com.example.memokeyword.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: Memo): Long

    @Update
    suspend fun updateMemo(memo: Memo)

    @Delete
    suspend fun deleteMemo(memo: Memo)

    @Query("SELECT * FROM memos ORDER BY updatedAt DESC")
    fun getAllMemos(): LiveData<List<Memo>>

    @Query("SELECT * FROM memos WHERE id = :memoId")
    suspend fun getMemoById(memoId: Long): Memo?

    @Transaction
    @Query("SELECT * FROM memos ORDER BY updatedAt DESC")
    fun getAllMemosWithKeywords(): LiveData<List<MemoWithKeywords>>

    @Transaction
    @Query("SELECT * FROM memos WHERE id = :memoId")
    suspend fun getMemoWithKeywords(memoId: Long): MemoWithKeywords?

    // 키워드로 메모 검색 (키워드 단어 포함)
    @Transaction
    @Query("""
        SELECT DISTINCT m.* FROM memos m
        INNER JOIN memo_keyword_cross_ref mkr ON m.id = mkr.memoId
        INNER JOIN keywords k ON mkr.keywordId = k.id
        WHERE k.word LIKE '%' || :keyword || '%'
        ORDER BY m.updatedAt DESC
    """)
    fun searchMemosByKeyword(keyword: String): LiveData<List<MemoWithKeywords>>

    // 제목 또는 내용으로 메모 검색
    @Transaction
    @Query("""
        SELECT * FROM memos
        WHERE title LIKE '%' || :query || '%'
        OR content LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchMemosByContent(query: String): LiveData<List<MemoWithKeywords>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeywordRef(crossRef: MemoKeywordCrossRef)

    @Query("DELETE FROM memo_keyword_cross_ref WHERE memoId = :memoId")
    suspend fun deleteKeywordRefsForMemo(memoId: Long)
}
