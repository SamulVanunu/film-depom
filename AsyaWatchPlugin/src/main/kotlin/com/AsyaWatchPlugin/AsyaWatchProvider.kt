package com.AsyaWatchPlugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document
import java.lang.Exception

class AsyaWatchProvider : MainAPI() {
    override var name = "AsyaWatch"
    override var mainUrl = "https://asyawatch.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvSeriesType.Anime)

    // Ana sayfa URL'leri
    private val mainCategories = listOf(
        "populer" to "Popüler Anime",
        "yeni" to "Yeni Bölümler",
        "film" to "Anime Filmler"
    )

    // Main page sections
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) mainUrl else "$mainUrl/page/$page"
        val doc = app.get(url).document

        val animeList = doc.select("div.post-body div.row div.col-lg-2, div.post-body div.row div.col-md-2").mapNotNull { element ->
            try {
                val title = element.select("h2 a, h3 a").text().ifEmpty { return@mapNotNull null }
                val href = element.select("h2 a, h3 a").attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val episode = element.select("span.episode, span.latest-episode").text()

                newAnimeSearchResponse(title, href, TvSeriesType.Anime) {
                    this.posterUrl = posterUrl
                    this.episode = episode
                }
            } catch (e: Exception) {
                null
            }
        }

        return newHomePageResponse(request.name, animeList)
    }

    // Search functionality
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document

        return doc.select("div.post-body div.row div.col-lg-2, div.post-body div.row div.col-md-2").mapNotNull { element ->
            try {
                val title = element.select("h2 a, h3 a").text().ifEmpty { return@mapNotNull null }
                val href = element.select("h2 a, h3 a").attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val episode = element.select("span.episode, span.latest-episode").text()

                newAnimeSearchResponse(title, href, TvSeriesType.Anime) {
                    this.posterUrl = posterUrl
                    this.episode = episode
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Load anime details
    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val title = doc.select("h1.entry-title, h1.anime-title").text()
        val posterUrl = doc.select("div.featured-image img, div.anime-poster img").attr("src").ifEmpty {
            doc.select("div.featured-image img, div.anime-poster img").attr("data-src")
        }
        val description = doc.select("div.entry-content p, div.anime-info p.synopsis").text()
        val genre = doc.select("div.entry-content span.genre a, div.anime-info span.genre a").map { it.text() }
        val status = when {
            doc.select("div.entry-content span.status:contains(Devam Ediyor), div.anime-info span.status:contains(Devam Ediyor)").isNotEmpty() -> TvSeriesStatus.Ongoing
            doc.select("div.entry-content span.status:contains(Tamamlandı), div.anime-info span.status:contains(Tamamlandı)").isNotEmpty() -> TvSeriesStatus.Completed
            else -> TvSeriesStatus.Completed
        }

        // Get episodes with better selector support
        val episodes = doc.select("div.eplister ul li, div.episode-list ul li").mapNotNull { element ->
            try {
                val epTitle = element.select("a span.epl-num, a span.ep-num").text()
                val epUrl = element.select("a").attr("href")
                if (epTitle.isNotEmpty() && epUrl.isNotEmpty()) {
                    Episode(epUrl, epTitle)
                } else null
            } catch (e: Exception) {
                null
            }
        }.reversed()

        return newTvSeriesLoadResponse(title, url, TvSeriesType.Anime) {
            this.posterUrl = posterUrl
            this.plot = description
            this.tags = genre
            this.status = status
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    // Load episode links
    override suspend fun loadLinks(
        url: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(url).document

        // Get video sources from iframe
        doc.select("div.player-embed iframe, div.player iframe, iframe[src]").forEach { iframe ->
            val videoUrl = iframe.attr("src")
            if (videoUrl.isNotEmpty()) {
                loadExtractor(videoUrl, url, subtitleCallback, callback)
            }
        }

        // Get embedded video sources from scripts
        doc.select("div.entry-content script, div.episode-content script").forEach { script ->
            val scriptText = script.html()
            
            // M3U8 links
            val videoUrlRegex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
            videoUrlRegex.findAll(scriptText).forEach { match ->
                val videoUrl = match.value
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = videoUrl,
                        referer = url,
                        quality = Qualities.P1080.value,
                        isM3u8 = true
                    )
                )
            }

            // MP4 links
            val mp4Regex = Regex("""(https?://[^\s"']+\.mp4[^\s"']*)""")
            mp4Regex.findAll(scriptText).forEach { match ->
                val videoUrl = match.value
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = videoUrl,
                        referer = url,
                        quality = Qualities.P1080.value,
                        isM3u8 = false
                    )
                )
            }
        }

        // Get subtitles
        doc.select("div.subtitles select option, div.player-subtitle select option").forEach { option ->
            val subUrl = option.attr("value")
            val subLang = option.text()
            if (subUrl.isNotEmpty()) {
                subtitleCallback.invoke(
                    SubtitleFile(
                        url = subUrl,
                        lang = subLang
                    )
                )
            }
        }

        return true
    }
}
