package com.HDFilmCehennemiPlugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document
import java.lang.Exception

class HDFilmCehennemiProvider : MainAPI() {
    override var name = "HDFilmCehennemi"
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvSeriesType.TvSeries, MovieType.Movie)

    // Main page sections
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page == 1) mainUrl else "$mainUrl/page/$page"
        val doc = app.get(url).document

        val movieList = doc.select("div.movie-box, div.film-box, article.movie-item").mapNotNull { element ->
            try {
                val title = element.select("h2 a, h3 a, a.title").text().ifEmpty { return@mapNotNull null }
                val href = element.select("h2 a, h3 a, a.title").attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val year = element.select("span.date, span.year").text()
                val rating = element.select("span.imdb, span.rating").text()
                val type = element.select("span.type, span.category").text()

                if (type.contains("Film") || type.contains("movie")) {
                    newMovieSearchResponse(title, href, MovieType.Movie) {
                        this.posterUrl = posterUrl
                        this.year = year.toIntOrNull()
                    }
                } else {
                    newTvSeriesSearchResponse(title, href, TvSeriesType.TvSeries) {
                        this.posterUrl = posterUrl
                        this.year = year.toIntOrNull()
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        return newHomePageResponse(request.name, movieList)
    }

    // Search functionality
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document

        return doc.select("div.movie-box, div.film-box, article.movie-item").mapNotNull { element ->
            try {
                val title = element.select("h2 a, h3 a, a.title").text().ifEmpty { return@mapNotNull null }
                val href = element.select("h2 a, h3 a, a.title").attr("href")
                val posterUrl = element.select("img").attr("src").ifEmpty {
                    element.select("img").attr("data-src")
                }
                val type = element.select("span.type, span.category").text()

                if (type.contains("Film") || type.contains("movie")) {
                    newMovieSearchResponse(title, href, MovieType.Movie) {
                        this.posterUrl = posterUrl
                    }
                } else {
                    newTvSeriesSearchResponse(title, href, TvSeriesType.TvSeries) {
                        this.posterUrl = posterUrl
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Load movie/series details
    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val title = doc.select("h1.entry-title, h1.film-title").text()
        val posterUrl = doc.select("div.featured-image img, div.film-poster img").attr("src").ifEmpty {
            doc.select("div.featured-image img, div.film-poster img").attr("data-src")
        }
        val description = doc.select("div.entry-content p, div.film-info p.synopsis").text()
        val genre = doc.select("div.entry-content span.genre a, div.film-info span.genre a").map { it.text() }
        val year = doc.select("div.entry-content span.date, div.film-info span.year").text()
        val imdbRating = doc.select("div.entry-content span.imdb, div.film-info span.rating").text()
        val type = doc.select("div.entry-content span.type, div.film-info span.category").text()

        // Check if it's a movie or series
        val isMovie = type.contains("Film") || type.contains("movie") || 
                     doc.select("div.entry-content span.type:contains(Film)").isNotEmpty()

        if (isMovie) {
            return newMovieLoadResponse(title, url, MovieType.Movie) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.year = year.toIntOrNull()
                this.rating = imdbRating.toFloatOrNull()?.times(10)?.toInt()
            }
        } else {
            // Get episodes for TV series
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

            return newTvSeriesLoadResponse(title, url, TvSeriesType.TvSeries) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.year = year.toIntOrNull()
                this.rating = imdbRating.toFloatOrNull()?.times(10)?.toInt()
                addEpisodes(DubStatus.Subbed, episodes)
            }
        }
    }

    // Load video links
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
        doc.select("div.entry-content script, div.film-content script").forEach { script ->
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
