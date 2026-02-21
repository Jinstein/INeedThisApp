package com.example.memokeyword.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object LinkContentFetcher {

    data class LinkContent(
        val title: String,
        val content: String,
        val url: String
    )

    suspend fun fetch(url: String): Result<LinkContent> = withContext(Dispatchers.IO) {
        runCatching {
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Android 14; Mobile)")
                .timeout(10_000)
                .get()

            val title = doc.title().trim()

            // 스크립트, 스타일, 내비게이션 등 불필요한 태그 제거
            doc.select("script, style, nav, footer, header, aside, iframe, noscript").remove()

            // 본문 텍스트 추출 (article > main > body 순으로 우선순위)
            val bodyText = when {
                doc.selectFirst("article") != null ->
                    doc.selectFirst("article")!!.wholeText()
                doc.selectFirst("main") != null ->
                    doc.selectFirst("main")!!.wholeText()
                else ->
                    doc.body().wholeText()
            }

            // 연속된 빈 줄 정리
            val cleanedContent = bodyText
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .take(3000) // 너무 긴 내용은 3000자로 제한

            LinkContent(
                title = title,
                content = cleanedContent,
                url = url
            )
        }
    }

    fun isValidUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://")
    }
}
