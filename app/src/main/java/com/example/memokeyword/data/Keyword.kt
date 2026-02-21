package com.example.memokeyword.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keywords")
data class Keyword(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String
)
