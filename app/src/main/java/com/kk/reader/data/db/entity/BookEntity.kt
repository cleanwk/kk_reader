package com.kk.reader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val filePath: String,
    val fileType: String, // "epub", "pdf", "txt"
    val coverPath: String? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val currentChapter: Int = 0,
    val progress: Float = 0f,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis()
)
