package com.kk.reader.domain.parser

import com.kk.reader.domain.model.Chapter
import com.kk.reader.domain.model.ParsedBook
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TxtParser @Inject constructor() : BookParser {

    companion object {
        private const val CHARS_PER_PAGE = 2000
        private val CHAPTER_PATTERN = Regex(
            """^(第[零一二三四五六七八九十百千万\d]+[章节回卷]|Chapter\s+\d+|CHAPTER\s+\d+).*""",
            RegexOption.MULTILINE
        )
    }

    override suspend fun parse(filePath: String): ParsedBook {
        val file = File(filePath)
        val text = readWithCharsetDetection(file)
        val title = file.nameWithoutExtension
        val chapters = splitIntoChapters(text)

        return ParsedBook(
            title = title,
            chapters = chapters,
            totalCharacters = text.length
        )
    }

    override suspend fun getChapter(filePath: String, index: Int): Chapter? {
        val text = readWithCharsetDetection(File(filePath))
        val chapters = splitIntoChapters(text)
        return chapters.getOrNull(index)
    }

    override suspend fun getPageText(filePath: String, page: Int): String {
        val text = readWithCharsetDetection(File(filePath))
        val chapters = splitIntoChapters(text)
        return getPageFromChapters(chapters, page, CHARS_PER_PAGE)
    }

    private fun readWithCharsetDetection(file: File): String {
        val bytes = file.readBytes()
        // Try UTF-8 first, then GBK for Chinese text
        return try {
            val text = String(bytes, Charsets.UTF_8)
            if (text.contains('\uFFFD')) {
                String(bytes, Charset.forName("GBK"))
            } else {
                text
            }
        } catch (_: Exception) {
            try {
                String(bytes, Charset.forName("GBK"))
            } catch (_: Exception) {
                String(bytes, Charsets.ISO_8859_1)
            }
        }
    }

    private fun splitIntoChapters(text: String): List<Chapter> {
        val matches = CHAPTER_PATTERN.findAll(text).toList()

        if (matches.isEmpty()) {
            // No chapter markers found — split by fixed size
            return splitBySize(text)
        }

        val chapters = mutableListOf<Chapter>()
        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val content = text.substring(start, end).trim()
            chapters.add(
                Chapter(
                    index = i,
                    title = matches[i].value.take(50).trim(),
                    content = content
                )
            )
        }

        // If there's content before the first chapter marker, prepend it
        if (matches.first().range.first > 0) {
            val preface = text.substring(0, matches.first().range.first).trim()
            if (preface.isNotEmpty()) {
                chapters.add(0, Chapter(index = 0, title = "Preface", content = preface))
                // Re-index
                return chapters.mapIndexed { idx, ch -> ch.copy(index = idx) }
            }
        }

        return chapters
    }

    private fun splitBySize(text: String): List<Chapter> {
        if (text.isEmpty()) return listOf(Chapter(0, "Empty", ""))
        val pages = mutableListOf<Chapter>()
        var offset = 0
        var index = 0
        while (offset < text.length) {
            val end = minOf(offset + CHARS_PER_PAGE, text.length)
            pages.add(
                Chapter(
                    index = index,
                    title = "Section ${index + 1}",
                    content = text.substring(offset, end)
                )
            )
            offset = end
            index++
        }
        return pages
    }
}
