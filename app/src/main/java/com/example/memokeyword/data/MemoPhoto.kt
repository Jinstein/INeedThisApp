package com.example.memokeyword.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memo_photos",
    foreignKeys = [
        ForeignKey(
            entity = Memo::class,
            parentColumns = ["id"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memoId")]
)
data class MemoPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoId: Long,
    val filePath: String,
    val orderIndex: Int = 0
)
