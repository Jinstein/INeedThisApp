package com.example.memokeyword.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MemoWithKeywords(
    @Embedded val memo: Memo,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoKeywordCrossRef::class,
            parentColumn = "memoId",
            entityColumn = "keywordId"
        )
    )
    val keywords: List<Keyword>,
    @Relation(
        parentColumn = "id",
        entityColumn = "memoId"
    )
    val photos: List<MemoPhoto> = emptyList()
)
