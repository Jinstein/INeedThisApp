package com.example.memokeyword.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoPhotoDao {

    @Insert
    suspend fun insertPhoto(photo: MemoPhoto): Long

    @Query("SELECT * FROM memo_photos WHERE memoId = :memoId ORDER BY orderIndex ASC")
    suspend fun getPhotosForMemo(memoId: Long): List<MemoPhoto>

    @Query("DELETE FROM memo_photos WHERE memoId = :memoId")
    suspend fun deletePhotosForMemo(memoId: Long)

    @Query("DELETE FROM memo_photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: Long)
}
