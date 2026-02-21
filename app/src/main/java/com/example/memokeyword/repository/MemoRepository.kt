package com.example.memokeyword.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.memokeyword.data.*
import com.example.memokeyword.util.KeywordExtractor
import java.io.File
import java.util.UUID

class MemoRepository(
    private val memoDao: MemoDao,
    private val keywordDao: KeywordDao,
    private val memoPhotoDao: MemoPhotoDao
) {

    val allMemosWithKeywords: LiveData<List<MemoWithKeywords>> =
        memoDao.getAllMemosWithKeywords()

    val allKeywords: LiveData<List<Keyword>> = keywordDao.getAllKeywords()

    suspend fun saveMemo(
        context: Context,
        title: String,
        content: String,
        userKeywords: List<String>,
        photoUris: List<Uri>,
        memoId: Long = 0
    ): Long {
        val now = System.currentTimeMillis()

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
            memoDao.deleteKeywordRefsForMemo(memoId)
            memoId
        }

        for (word in allKeywords) {
            if (word.isBlank()) continue
            val existing = keywordDao.getKeywordByWord(word)
            val keywordId = existing?.id ?: keywordDao.insertKeyword(Keyword(word = word))
            if (keywordId > 0) {
                memoDao.insertKeywordRef(MemoKeywordCrossRef(savedMemoId, keywordId))
            }
        }

        keywordDao.deleteOrphanKeywords()

        // 기존 사진 파일들 삭제 후 재저장
        if (memoId != 0L) {
            val existingPhotos = memoPhotoDao.getPhotosForMemo(memoId)
            existingPhotos.forEach { File(it.filePath).delete() }
            memoPhotoDao.deletePhotosForMemo(memoId)
        }

        val photoDir = File(context.filesDir, "memo_photos").also { it.mkdirs() }
        photoUris.take(5).forEachIndexed { index, uri ->
            val destFile = File(photoDir, "${UUID.randomUUID()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                memoPhotoDao.insertPhoto(
                    MemoPhoto(memoId = savedMemoId, filePath = destFile.absolutePath, orderIndex = index)
                )
            } catch (e: Exception) {
                destFile.delete()
            }
        }

        return savedMemoId
    }

    suspend fun deleteMemo(memo: Memo) {
        val photos = memoPhotoDao.getPhotosForMemo(memo.id)
        photos.forEach { File(it.filePath).delete() }
        memoDao.deleteMemo(memo)
        keywordDao.deleteOrphanKeywords()
    }

    suspend fun getMemoWithKeywords(memoId: Long): MemoWithKeywords? {
        return memoDao.getMemoWithKeywords(memoId)
    }

    suspend fun getPhotosForMemo(memoId: Long): List<MemoPhoto> {
        return memoPhotoDao.getPhotosForMemo(memoId)
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
