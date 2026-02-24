package com.kk.reader.domain.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookParserFactory @Inject constructor(
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val txtParser: TxtParser
) {
    fun getParser(filePath: String): BookParser {
        val ext = filePath.substringAfterLast('.').lowercase()
        return when (ext) {
            "epub" -> epubParser
            "pdf" -> pdfParser
            "txt" -> txtParser
            else -> txtParser
        }
    }
}
