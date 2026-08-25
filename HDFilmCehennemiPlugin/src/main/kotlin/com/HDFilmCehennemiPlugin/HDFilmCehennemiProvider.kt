package com.HDFilmCehennemiPlugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.utils.*
import java.lang.Exception

@CloudstreamPlugin
class HDFilmCehennemiProvider : MainAPI() {
    override var name = "HDFilmCehennemi"
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) mainUrl else "$mainUrl/page/$page"
        val doc = app.get(url).document

        val movieList = doc.select("div.movie-box, div.film-box, article.movie-item").mapNotNull { element ->
            try {
                val titleEl = element.select("h2 a, h3 a, a.title").firstOrNull() ?: return@mapNotNull null
                val title = titleEl.text().ifEmpty { return@mapNotNull null }
                val href = titleEl.attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val type = element.select("span.type, span.category").text()

                if (type.contains("Film") || type.contains("movie")) {
                    newMovieSearchResponse(title, href, TvType.Movie) {
                        this.posterUrl = posterUrl
                    }
                } else {
                    newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                        this.posterUrl = posterUrl
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        return newHomePageResponse(request.name, movieList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document

        return doc.select("div.movie-box, div.film-box, article.movie-item").mapNotNull { element ->
            try {
                val titleEl = element.select("h2 a, h3 a, a.title").firstOrNull() ?: return@mapNotNull null
                val title = titleEl.text().ifEmpty { return@mapNotNull null }
                val href = titleEl.attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val type = element.select("span.type, span.category").text()

                if (type.contains("Film") || type.contains("movie")) {
                    newMovieSearchResponse(title, href, TvType.Movie) {
                        this.posterUrl = posterUrl
                    }
                } else {
                    newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                        this.posterUrl = posterUrl
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun load(data: String): LoadResponse? {
        val doc = app.get(data).document

        val title = doc.select("h1.entry-title, h1.film-title").text()
        val posterUrl = doc.select("div.featured-image img, div.film-poster img").attr("src").ifEmpty {
            doc.select("div.featured-image img, div.film-poster img").attr("data-src")
        }
        val description = doc.select("div.entry-content p, div.film-info p.synopsis").text()
        val genre = doc.select("div.entry-content span.genre a, div.film-info span.genre a").map { it.text() }
        val year = doc.select("div.entry-content span.date, div.film-info span.year").text()
        val imdbRating = doc.select("div.entry-content span.imdb, div.film-info span.rating").text()
        val type = doc.select("div.entry-content span.type, div.film-info span.category").text()

        val isMovie = type.contains("Film") || type.contains("movie") ||
                     doc.select("span.type:contains(Film)").isNotEmpty()

        if (isMovie) {
            return newMovieLoadResponse(title, data, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.year = year.toIntOrNull()
            }
        } else {
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

            return newTvSeriesLoadResponse(title, data, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.year = year.toIntOrNull()
            }
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

        doc.select("div.entry-content script, div.film-content script").forEach { script ->
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
