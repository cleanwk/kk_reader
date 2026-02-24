package com.kk.reader.domain.parser

import com.kk.reader.domain.model.Chapter
import com.kk.reader.domain.model.ParsedBook
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfParser @Inject constructor() : BookParser {

    override suspend fun parse(filePath: String): ParsedBook {
        val doc = PDDocument.load(File(filePath))
        val info = doc.documentInformation
        val stripper = PDFTextStripper()
        val totalPages = doc.numberOfPages

        val chapters = (1..totalPages).map { pageNum ->
            stripper.startPage = pageNum
            stripper.endPage = pageNum
            val text = stripper.getText(doc).trim()
            Chapter(
                index = pageNum - 1,
                title = "Page $pageNum",
                content = text
            )
        }

        val title = info?.title?.takeIf { it.isNotBlank() }
            ?: File(filePath).nameWithoutExtension
        val author = info?.author ?: ""

        doc.close()

        return ParsedBook(
            title = title,
            author = author,
            chapters = chapters,
            totalCharacters = chapters.sumOf { it.content.length }
        )
    }

    override suspend fun getChapter(filePath: String, index: Int): Chapter? {
        val doc = PDDocument.load(File(filePath))
        val pageNum = index + 1
        if (pageNum > doc.numberOfPages) {
            doc.close()
            return null
        }
        val stripper = PDFTextStripper()
        stripper.startPage = pageNum
        stripper.endPage = pageNum
        val text = stripper.getText(doc).trim()
        doc.close()
        return Chapter(index = index, title = "Page $pageNum", content = text)
    }

    override suspend fun getPageText(filePath: String, page: Int): String {
        return getChapter(filePath, page)?.content ?: ""
    }
}
