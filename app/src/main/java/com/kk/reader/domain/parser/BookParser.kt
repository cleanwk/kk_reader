package com.kk.reader.domain.parser

import com.kk.reader.domain.model.Chapter
import com.kk.reader.domain.model.ParsedBook

interface BookParser {
    suspend fun parse(filePath: String): ParsedBook
    suspend fun getChapter(filePath: String, index: Int): Chapter?
    suspend fun getPageText(filePath: String, page: Int): String
}
