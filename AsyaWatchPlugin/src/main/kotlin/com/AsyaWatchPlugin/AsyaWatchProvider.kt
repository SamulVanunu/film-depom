package com.AsyaWatchPlugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.utils.*
import java.lang.Exception

@CloudstreamPlugin
class AsyaWatchProvider : MainAPI() {
    override var name = "AsyaWatch"
    override var mainUrl = "https://asyawatch.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) mainUrl else "$mainUrl/page/$page"
        val doc = app.get(url).document

        val animeList = doc.select("div.post-body div.row div.col-lg-2, div.post-body div.row div.col-md-2").mapNotNull { element ->
            try {
                val titleEl = element.select("h2 a, h3 a").firstOrNull() ?: return@mapNotNull null
                val title = titleEl.text().ifEmpty { return@mapNotNull null }
                val href = titleEl.attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }

                newAnimeSearchResponse(title, href, TvType.Anime) {
                    this.posterUrl = posterUrl
                }
            } catch (e: Exception) {
                null
            }
        }

        return newHomePageResponse(request.name, animeList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document

        return doc.select("div.post-body div.row div.col-lg-2, div.post-body div.row div.col-md-2").mapNotNull { element ->
            try {
                val titleEl = element.select("h2 a, h3 a").firstOrNull() ?: return@mapNotNull null
                val title = titleEl.text().ifEmpty { return@mapNotNull null }
                val href = titleEl.attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }

                newAnimeSearchResponse(title, href, TvType.Anime) {
                    this.posterUrl = posterUrl
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun load(data: String): LoadResponse? {
        val doc = app.get(data).document

        val title = doc.select("h1.entry-title, h1.anime-title").text()
        val posterUrl = doc.select("div.featured-image img, div.anime-poster img").attr("src").ifEmpty {
            doc.select("div.featured-image img, div.anime-poster img").attr("data-src")
        }
        val description = doc.select("div.entry-content p, div.anime-info p.synopsis").text()
        val genre = doc.select("div.entry-content span.genre a, div.anime-info span.genre a").map { it.text() }
        val status = when {
            doc.select("span.status:contains(Devam Ediyor)").isNotEmpty() -> ShowStatus.Ongoing
            doc.select("span.status:contains(Tamamlandı)").isNotEmpty() -> ShowStatus.Completed
            else -> ShowStatus.Completed
        }

        val episodes = doc.select("div.eplister ul li, div.episode-list ul li").mapNotNull { element ->
            try {
                val epTitle = element.select("a span.epl-num, a span.ep-num").text()
                val epUrl = element.select("a").attr("href")
                if (epTitle.isNotEmpty() && epUrl.isNotEmpty()) {
                    newEpisode(epUrl) {
                        this.name = epTitle
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }.reversed()

        return newAnimeLoadResponse(title, data, TvType.Anime) {
            this.posterUrl = posterUrl
            this.plot = description
            this.tags = genre
            this.showStatus = status
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        doc.select("div.player-embed iframe, div.player iframe, iframe[src]").forEach { iframe ->
            val videoUrl = iframe.attr("src")
            if (videoUrl.isNotEmpty()) {
                loadExtractor(videoUrl, data, subtitleCallback, callback)
            }
        }

        doc.select("div.entry-content script, div.episode-content script").forEach { script ->
            val scriptText = script.html()

            val videoUrlRegex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
            videoUrlRegex.findAll(scriptText).forEach { match ->
                val videoUrl = match.value
                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = videoUrl,
                        type = ExtractorLinkType.M3U8
                    ) {
                        quality = Qualities.P1080.value
                        referer = data
                    }
                )
            }

            val mp4Regex = Regex("""(https?://[^\s"']+\.mp4[^\s"']*)""")
            mp4Regex.findAll(scriptText).forEach { match ->
                val videoUrl = match.value
                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = videoUrl,
                        type = ExtractorLinkType.VIDEO
                    ) {
                        quality = Qualities.P1080.value
                        referer = data
                    }
                )
            }
        }

        doc.select("div.subtitles select option, div.player-subtitle select option").forEach { option ->
            val subUrl = option.attr("value")
            val subLang = option.text()
            if (subUrl.isNotEmpty()) {
                subtitleCallback.invoke(
                    newSubtitleFile(subUrl, subLang)
                )
            }
        }

        return true
    }
}
