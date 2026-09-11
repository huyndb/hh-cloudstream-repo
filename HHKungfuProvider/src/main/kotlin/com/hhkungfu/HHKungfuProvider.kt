package com.hhkungfu

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HHKungfuProvider : MainAPI() {
    override var mainUrl = "https://hhkungfu.ee"
    override var name = "HHKungfu"
    override var lang = "vi"
    override val supportedTypes = setOf(TvType.Anime)
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "latest" to "Mới cập nhật",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) mainUrl else "$mainUrl/page/$page"
        val items = HHKungfuParser.parseCatalog(app.get(url).text, mainUrl)
            .map { it.toSearchResponse() }
        return newHomePageResponse(request.name, items, hasNext = items.isNotEmpty())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val needle = HHKungfuParser.normalizeForSearch(query)
        if (needle.isBlank()) return emptyList()

        val found = linkedMapOf<String, CatalogItem>()
        // HHKungfu exposes paged catalog pages; scan a bounded window so search does not hammer the site.
        for (page in 1..5) {
            val url = if (page == 1) mainUrl else "$mainUrl/page/$page"
            val html = try {
                app.get(url).text
            } catch (_: Throwable) {
                continue
            }
            HHKungfuParser.parseCatalog(html, mainUrl)
                .filter { HHKungfuParser.normalizeForSearch(it.title).contains(needle) }
                .forEach { found[it.url] = it }
        }
        return found.values.map { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val detail = HHKungfuParser.parseDetail(app.get(url).text, mainUrl, url)
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

    /**
     * Playback extraction is intentionally not implemented. This provider indexes publicly visible
     * catalog/metadata/episode pages only and does not bypass DRM, anti-bot, token protection,
     * iframe obfuscation, or protected streaming mechanisms.
     */
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
