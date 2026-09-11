package com.hh3d

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HH3DProvider : MainAPI() {
    override var mainUrl = "https://www.hh3d.com"
    override var name = "HH3D"
    override var lang = "vi"
    override val supportedTypes = setOf(TvType.Anime)
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "latest" to "Mới cập nhật",
        "completed" to "Đã hoàn thành",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = when (request.data) {
            "completed" -> "$mainUrl/danh-sach/hoan-thanh"
            else -> mainUrl
        }
        if (page > 1) return newHomePageResponse(request.name, emptyList(), hasNext = false)
        val items = HH3DParser.parseCatalog(app.get(url).text, mainUrl)
            .map { it.toSearchResponse() }
        return newHomePageResponse(request.name, items, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val needle = HH3DParser.normalizeForSearch(query)
        if (needle.isBlank()) return emptyList()

        val urls = listOf(
            "$mainUrl/tu-khoa/${HH3DParser.slugify(query)}",
            mainUrl,
            "$mainUrl/danh-sach/hoan-thanh",
        )
        val found = linkedMapOf<String, CatalogItem>()
        for (url in urls) {
            val html = try {
                app.get(url).text
            } catch (_: Throwable) {
                continue
            }
            HH3DParser.parseCatalog(html, mainUrl)
                .filter { HH3DParser.normalizeForSearch(it.title).contains(needle) }
                .forEach { found[it.url] = it }
        }
        return found.values.map { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val detail = HH3DParser.parseDetail(app.get(url).text, mainUrl, url)
        val subbed = detail.episodes.filterNot { it.dubbed }.map { ep ->
            newEpisode(ep.url) {
                name = ep.name
                episode = ep.number
            }
        }
        val dubbed = detail.episodes.filter { it.dubbed }.map { ep ->
            newEpisode(ep.url) {
                name = ep.name
                episode = ep.number
            }
        }

        return newAnimeLoadResponse(detail.title, url, TvType.Anime) {
            engName = detail.altTitle
            posterUrl = detail.posterUrl
            year = detail.year
            plot = detail.plot
            tags = detail.tags
            if (subbed.isNotEmpty()) addEpisodes(DubStatus.Subbed, subbed)
            if (dubbed.isNotEmpty()) addEpisodes(DubStatus.Dubbed, dubbed)
        }
    }

    /** See HHKungfuProvider.loadLinks: catalog indexing only; no protected stream extraction. */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean = false

    private fun CatalogItem.toSearchResponse(): SearchResponse =
        newAnimeSearchResponse(title, url, TvType.Anime) {
            this.posterUrl = this@toSearchResponse.posterUrl
        }
}
