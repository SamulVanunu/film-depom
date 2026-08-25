package com.OpenAnimePlugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document
import java.lang.Exception

class OpenAnimeProvider : MainAPI() {
    override var name = "OpenAnime"
    override var mainUrl = "https://openani.me"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvSeriesType.Anime)

    // Main page sections
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) mainUrl else "$mainUrl/page/$page"
        val doc = app.get(url).document

        val animeList = doc.select("div.anime-card, div.card, article.anime-item").mapNotNull { element ->
            try {
                val title = element.select("h3 a, h2 a, a.title").text().ifEmpty { return@mapNotNull null }
                val href = element.select("h3 a, h2 a, a.title").attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val episode = element.select("span.episode, span.latest-episode").text()
                val type = element.select("span.type, span.category").text()

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

        return doc.select("div.anime-card, div.card, article.anime-item").mapNotNull { element ->
            try {
                val title = element.select("h3 a, h2 a, a.title").text().ifEmpty { return@mapNotNull null }
                val href = element.select("h3 a, h2 a, a.title").attr("href")
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

        val title = doc.select("h1.anime-title, h1.entry-title").text()
        val posterUrl = doc.select("div.anime-poster img, div.featured-image img").attr("src").ifEmpty {
            doc.select("div.anime-poster img, div.featured-image img").attr("data-src")
        }
        val description = doc.select("div.anime-info p.synopsis, div.entry-content p").text()
        val genre = doc.select("div.anime-info span.genre a, div.entry-content span.genre a").map { it.text() }
        val status = when {
            doc.select("div.anime-info span.status:contains(Devam Ediyor), div.entry-content span.status:contains(Devam Ediyor)").isNotEmpty() -> TvSeriesStatus.Ongoing
            doc.select("div.anime-info span.status:contains(Tamamlandı), div.entry-content span.status:contains(Tamamlandı)").isNotEmpty() -> TvSeriesStatus.Completed
            else -> TvSeriesStatus.Completed
        }
        val alternativeTitles = doc.select("div.anime-info div.alternative-titles span, div.entry-content div.alternative-titles span").map { it.text() }

        // Get episodes with better selector support
        val episodes = doc.select("div.episode-list ul li, div.eplister ul li").mapNotNull { element ->
            try {
                val epTitle = element.select("a span.ep-num, a span.epl-num").text()
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
            this.alternativeNames = alternativeTitles
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
        doc.select("div.player iframe, div.player-embed iframe, iframe[src]").forEach { iframe ->
            val videoUrl = iframe.attr("src")
            if (videoUrl.isNotEmpty()) {
                loadExtractor(videoUrl, url, subtitleCallback, callback)
            }
        }

        // Get embedded video sources from scripts
        doc.select("div.episode-content script, div.entry-content script").forEach { script ->
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
