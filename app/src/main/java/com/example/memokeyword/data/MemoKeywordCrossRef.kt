package com.example.memokeyword.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memo_keyword_cross_ref",
    primaryKeys = ["memoId", "keywordId"],
    foreignKeys = [
        ForeignKey(
            entity = Memo::class,
            parentColumns = ["id"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Keyword::class,
            parentColumns = ["id"],
            childColumns = ["keywordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("keywordId")]
)
data class MemoKeywordCrossRef(
    val memoId: Long,
    val keywordId: Long
)
