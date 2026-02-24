package com.kk.reader.domain.parser

import android.graphics.BitmapFactory
import com.kk.reader.domain.model.Chapter
import com.kk.reader.domain.model.ParsedBook
import nl.siegmann.epublib.epub.EpubReader
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubParser @Inject constructor() : BookParser {

    override suspend fun parse(filePath: String): ParsedBook {
        val book = EpubReader().readEpub(FileInputStream(filePath))
        val metadata = book.metadata

        val title = metadata.firstTitle ?: "Unknown"
        val author = metadata.authors.firstOrNull()?.let {
            "${it.firstname} ${it.lastname}".trim()
        } ?: ""

        val coverImage = book.coverImage?.let { cover ->
            try {
                BitmapFactory.decodeByteArray(cover.data, 0, cover.data.size)
            } catch (_: Exception) {
                null
            }
        }

        val chapters = book.spine.spineReferences.mapIndexed { index, ref ->
            val resource = ref.resource
            val html = String(resource.data, Charsets.UTF_8)
            val text = stripHtml(html)
            val chapterTitle = resource.title ?: "Chapter ${index + 1}"
            Chapter(
                index = index,
                title = chapterTitle,
                content = text,
                htmlContent = html
            )
        }

        return ParsedBook(
            title = title,
            author = author,
            coverImage = coverImage,
            chapters = chapters,
            totalCharacters = chapters.sumOf { it.content.length }
        )
    }

    override suspend fun getChapter(filePath: String, index: Int): Chapter? {
        val book = EpubReader().readEpub(FileInputStream(filePath))
        val refs = book.spine.spineReferences
        if (index !in refs.indices) return null
        val resource = refs[index].resource
        val html = String(resource.data, Charsets.UTF_8)
        return Chapter(
            index = index,
            title = resource.title ?: "Chapter ${index + 1}",
            content = stripHtml(html),
            htmlContent = html
        )
    }

    override suspend fun getPageText(filePath: String, page: Int): String {
        val parsed = parse(filePath)
        return getPageFromChapters(parsed.chapters, page)
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>"), "")
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<p[^>]*>"), "\n")
            .replace(Regex("</p>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}

internal fun getPageFromChapters(chapters: List<Chapter>, page: Int, charsPerPage: Int = 2000): String {
    val allText = chapters.joinToString("\n\n") { "${it.title}\n\n${it.content}" }
    val start = page * charsPerPage
    if (start >= allText.length) return ""
    val end = minOf(start + charsPerPage, allText.length)
    return allText.substring(start, end)
}

internal fun totalPages(totalChars: Int, charsPerPage: Int = 2000): Int {
    return if (totalChars == 0) 1 else (totalChars + charsPerPage - 1) / charsPerPage
}
