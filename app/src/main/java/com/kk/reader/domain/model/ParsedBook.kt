package com.kk.reader.domain.model

import android.graphics.Bitmap

data class ParsedBook(
    val title: String,
    val author: String = "",
    val coverImage: Bitmap? = null,
    val chapters: List<Chapter> = emptyList(),
    val totalCharacters: Int = 0
)

data class Chapter(
    val index: Int,
    val title: String,
    val content: String,
    val htmlContent: String? = null
)
