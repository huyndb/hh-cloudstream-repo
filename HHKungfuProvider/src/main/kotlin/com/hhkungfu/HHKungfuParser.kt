package com.hhkungfu

data class CatalogItem(
    val title: String,
    val url: String,
    val posterUrl: String? = null,
)

data class EpisodeItem(
    val name: String,
    val url: String,
    val number: Int?,
    val dubbed: Boolean,
)

data class DetailItem(
    val title: String,
    val altTitle: String? = null,
    val posterUrl: String? = null,
    val year: Int? = null,
    val plot: String? = null,
    val tags: List<String> = emptyList(),
    val episodes: List<EpisodeItem> = emptyList(),
)

object HHKungfuParser {
    private val anchorRegex = Regex("""(?is)<a\b[^>]*href\s*=\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>""")
    private val imageRegex = Regex("""(?is)<img\b[^>]*(?:src|data-src)\s*=\s*[\"']([^\"']+)[\"'][^>]*>""")
    private val altRegex = Regex("""(?is)\balt\s*=\s*[\"']([^\"']*)[\"']""")

    fun parseCatalog(html: String, mainUrl: String): List<CatalogItem> =
        anchorRegex.findAll(html).mapNotNull { match ->
            val href = match.groupValues[1].trim()
            if (!isDetailHref(href)) return@mapNotNull null
            val inner = match.groupValues[2]
            val imgTag = Regex("""(?is)<img\b[^>]*>""").find(inner)?.value
            val title = imgTag?.let { altRegex.find(it)?.groupValues?.getOrNull(1) }
                ?.decodeHtml()?.trim()?.takeIf { it.isNotBlank() }
                ?: stripTags(inner).trim().takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val poster = imageRegex.find(inner)?.groupValues?.getOrNull(1)?.let { absoluteUrl(mainUrl, it) }
            CatalogItem(cleanCardTitle(title), absoluteUrl(mainUrl, href), poster)
        }.distinctBy { it.url }.toList()

    fun parseDetail(html: String, mainUrl: String, pageUrl: String): DetailItem {
        val title = extractTagText(html, "h1") ?: pageUrl.substringAfterLast('/').replace('-', ' ')
        val altTitle = extractTagText(html, "h2")
        val poster = extractMeta(html, "og:image") ?: imageRegex.find(html)?.groupValues?.getOrNull(1)?.let { absoluteUrl(mainUrl, it) }
        val year = Regex("""\b(20\d{2})\b""").find(stripTags(html).take(5000))?.groupValues?.get(1)?.toIntOrNull()
        val plot = extractPlot(html)
        val tags = extractGenreLinks(html)
        val dubbedBoundary = Regex("""(?i)Thuy.{0,5}t\s*Minh""").find(html)?.range?.first ?: Int.MAX_VALUE
        val episodes = anchorRegex.findAll(html).mapNotNull { match ->
            val href = match.groupValues[1].trim()
            if (!Regex("""(?i)(?:^|/)watch-[^/]+/tap-[^/]+\.html(?:$|[?#])""").containsMatchIn(href)) return@mapNotNull null
            val label = stripTags(match.groupValues[2]).trim()
            val number = Regex("""(?i)tap\s*([0-9]+)""").find(label)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(?i)/tap-([0-9]+)""").find(href)?.groupValues?.get(1)?.toIntOrNull()
            val dubbedByUrl = Regex("""(?i)(?:-tm|-lt|-long-tieng|thuyet-minh)""").containsMatchIn(href)
            val dubbed = dubbedByUrl || match.range.first > dubbedBoundary
            EpisodeItem(
                name = buildEpisodeName(number, dubbed, label),
                url = absoluteUrl(mainUrl, href),
                number = number,
                dubbed = dubbed,
            )
        }.distinctBy { it.url }.sortedWith(compareBy<EpisodeItem> { it.number ?: Int.MAX_VALUE }.thenBy { it.dubbed }).toList()

        return DetailItem(
            title = title,
            altTitle = altTitle?.takeIf { it != title },
            posterUrl = poster,
            year = year,
            plot = plot,
            tags = tags,
            episodes = episodes,
        )
    }

    fun normalizeForSearch(value: String): String = java.text.Normalizer
        .normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun isDetailHref(href: String): Boolean {
        val path = href.substringBefore('?').substringBefore('#')
        if (path.startsWith("http", ignoreCase = true)) {
            val afterHost = path.substringAfter("//").substringAfter('/', "")
            return isSingleSlug(afterHost)
        }
        return isSingleSlug(path.trim('/'))
    }

    private fun isSingleSlug(path: String): Boolean {
        if (path.isBlank() || '/' in path) return false
        val blocked = setOf("sitemap.xml", "contact", "login", "register", "page", "the-loai", "tag")
        if (path.lowercase() in blocked) return false
        return !path.startsWith("watch-", ignoreCase = true) && !path.startsWith("page", ignoreCase = true)
    }

    private fun cleanCardTitle(value: String): String = value
        .replace(Regex("(?i)^FULL\\s*HD\\s*4K\\s*"), "")
        .replace(Regex("(?i)^FULL\\s*HD\\s*"), "")
        .replace(Regex("(?i)^4K\\s*"), "")
        .trim()

    private fun extractTagText(html: String, tag: String): String? =
        Regex("""(?is)<$tag\b[^>]*>(.*?)</$tag>""").find(html)?.groupValues?.getOrNull(1)
            ?.let(::stripTags)?.trim()?.takeIf { it.isNotBlank() }

    private fun extractMeta(html: String, property: String): String? {
        val tag = Regex("""(?is)<meta\b[^>]*(?:property|name)\s*=\s*[\"']${Regex.escape(property)}[\"'][^>]*>""").find(html)?.value ?: return null
        return Regex("""(?is)content\s*=\s*[\"']([^\"']+)[\"']""").find(tag)?.groupValues?.getOrNull(1)?.decodeHtml()
    }

    private fun extractPlot(html: String): String? {
        val match = Regex("""(?is)<h[1-6]\b[^>]*>\s*(?:<[^>]+>)*\s*Nội\s*dung\s*phim\s*(?:</[^>]+>)*\s*</h[1-6]>\s*(?:<[^>]+>\s*)*<p\b[^>]*>(.*?)</p>""").find(html)
        return match?.groupValues?.getOrNull(1)?.let(::stripTags)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractGenreLinks(html: String): List<String> {
        val genreBlock = Regex("""(?is)Thể\s*Loại\s*:?(.*?)(?:Tập\s*mới\s*nhất|Tình\s*trạng|<h[1-6])""").find(html)?.groupValues?.getOrNull(1) ?: return emptyList()
        return anchorRegex.findAll(genreBlock).map { stripTags(it.groupValues[2]).trim() }.filter { it.isNotBlank() }.distinct().toList()
    }

    private fun buildEpisodeName(number: Int?, dubbed: Boolean, fallback: String): String {
        val mode = if (dubbed) "Thuyết minh" else "Vietsub"
        return number?.let { "Tập $it • $mode" } ?: "$fallback • $mode"
    }

    private fun stripTags(value: String): String = value
        .replace(Regex("(?is)<script\\b.*?</script>"), " ")
        .replace(Regex("(?is)<style\\b.*?</style>"), " ")
        .replace(Regex("(?is)<[^>]+>"), " ")
        .decodeHtml()
        .replace(Regex("\\s+"), " ")

    private fun String.decodeHtml(): String = this
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private fun absoluteUrl(mainUrl: String, href: String): String = when {
        href.startsWith("https://", true) || href.startsWith("http://", true) -> href
        href.startsWith("//") -> "https:$href"
        href.startsWith("/") -> mainUrl.trimEnd('/') + href
        else -> mainUrl.trimEnd('/') + "/" + href
    }
}
