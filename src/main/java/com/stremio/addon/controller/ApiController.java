
package com.stremio.addon.controller;

import com.stremio.addon.controller.dto.MovieDto;
import com.stremio.addon.controller.dto.ProviderDto;
import com.stremio.addon.controller.dto.ResultsDto;
import com.stremio.addon.controller.dto.SeriesDto;
import com.stremio.addon.service.FavoritesService;
import com.stremio.addon.service.transmission.TransmissionService;
import com.stremio.addon.service.tmdb.TmdbService;
import com.stremio.addon.service.transmission.dto.TorrentInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/tmdb", produces = "application/json")
public class ApiController {

    private final TmdbService tmdbService;
    private final FavoritesService favoritesService;
    private final TransmissionService transmissionService;

    public ApiController(TmdbService tmdbService, FavoritesService favoritesService, TransmissionService transmissionService) {
        this.tmdbService = tmdbService;
        this.favoritesService = favoritesService;
        this.transmissionService = transmissionService;
    }

    /**
     * Get the title of a movie or TV show by IMDb ID.
     */
    @GetMapping("/title")
    public ResponseEntity<String> getTitle(
            @RequestParam String imdbId,
            @RequestParam String contentType) {
        String title = tmdbService.getTitle(imdbId, contentType);
        return ResponseEntity.ok(title);
    }

    /**
     * Get details of a movie by TMDB ID.
     */
    @GetMapping("/movie/{tmdbId}")
    public ResponseEntity<?> getMovieDetail(@PathVariable int tmdbId) {
        var movieDetail = tmdbService.getMovieDetail(tmdbId);
        return ResponseEntity.ok(movieDetail);
    }

    /**
     * Get details of a TV show by TMDB ID.
     */
    @GetMapping("/tv/{tvShowId}")
    public ResponseEntity<?> getTvShow(@PathVariable int tvShowId) {
        var tvShowDetail = tmdbService.getTvShowDetail(tvShowId);
        return ResponseEntity.ok(tvShowDetail);
    }

    /**
     * Get details of a TV show by TMDB ID.
     */
    @GetMapping("/tv/{tvShowId}/season/{season}")
    public ResponseEntity<?> getSeasonDetail(@PathVariable int tvShowId, @PathVariable int season) {
        var tvShowDetail = tmdbService.getSeasonDetails(tvShowId, season);
        return ResponseEntity.ok(tvShowDetail);
    }

    @GetMapping("/movie/torrents/{movieId}")
    public ResponseEntity<?> getTorrentsMovie(@PathVariable int movieId) {
        var torrents = tmdbService.getTorrentsForMovie(movieId);
        return ResponseEntity.ok(torrents);
    }

    @GetMapping("/tv/torrents/{tvShowId}/season/{season}/episode/{episode}")
    public ResponseEntity<?> getTorrentsSeries(@PathVariable int tvShowId, @PathVariable int season, @PathVariable int episode) {
        var torrents = tmdbService.getTorrentsForTvShow(tvShowId, season, episode);
        return ResponseEntity.ok(torrents);
    }

    /**
     * Get trending movies with pagination.
     */
    @GetMapping("/trending/movies")
    public ResponseEntity<?> getTrendingMovies(@RequestParam(defaultValue = "1") int page) {
        var trendingMovies = tmdbService.getTrendingMovies(page);
        return ResponseEntity.ok(trendingMovies);
    }

    /**
     * Get trending TV shows with pagination.
     */
    @GetMapping("/trending/tv")
    public ResponseEntity<?> getTrendingTvShows(@RequestParam(defaultValue = "1") int page) {
        var trendingTvShows = tmdbService.getTrendingTvShows(page);
        return ResponseEntity.ok(trendingTvShows);
    }

    /**
     * Get favorite movies with pagination and sorting.
     */
    @GetMapping("/favorites/movies")
    public ResponseEntity<?> getFavoriteMovies(
            @RequestParam int page,
            @RequestParam(defaultValue = "popularity.desc") String sortBy) {
        var favoriteMovies = tmdbService.getFavoriteMovies(page, sortBy);
        return ResponseEntity.ok(favoriteMovies);
    }

    /**
     * Get favorite TV shows with pagination and sorting.
     */
    @GetMapping("/favorites/tv")
    public ResponseEntity<?> getFavoriteTvShows(
            @RequestParam int page,
            @RequestParam(defaultValue = "created_at.asc") String sortBy) {
        var favoriteTvShows = tmdbService.getFavoriteTvShows(page, sortBy);
        return ResponseEntity.ok(favoriteTvShows);
    }

    /**
     * Mark or unmark a movie or TV show as a favorite.
     */
    @PostMapping("/favorites")
    public ResponseEntity<Void> markAsFavorite(
            @RequestParam long mediaId,
            @RequestParam String mediaType,
            @RequestParam boolean favorite) {
        favoritesService.manageFavorite(mediaId, mediaType, favorite);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para buscar películas con filtros avanzados.
     *
     * @param filters Filtros personalizados de búsqueda (opcional).
     * @return Lista paginada de películas que coinciden con los filtros.
     */
    @GetMapping("/discover/movies")
    public ResponseEntity<ResultsDto<MovieDto>> discoverMovies(
            @RequestParam Map<String, String> filters) {
        var movies = tmdbService.discoverMovies(filters);
        return ResponseEntity.ok(movies);
    }

    /**
     * Endpoint para buscar series con filtros avanzados.
     *
     * @param filters Filtros personalizados de búsqueda (opcional).
     * @return Lista paginada de series que coinciden con los filtros.
     */
    @GetMapping("/discover/tv")
    public ResponseEntity<ResultsDto<SeriesDto>> discoverTvShows(
            @RequestParam Map<String, String> filters) {
        var tvShows = tmdbService.discoverTvShows(filters);
        return ResponseEntity.ok(tvShows);
    }

    @GetMapping("/watch/providers/tv")
    public ResponseEntity<?> getTvWatchProviders() {
        var response = tmdbService.getTvWatchProviders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/watch/providers/movies")
    public ResponseEntity<?> getMovieProviders() {
        var response = tmdbService.getMovieProviders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("{tv}/watch/providers/tv")
    public ResponseEntity<?> getTvWatchProviders(@PathVariable("tv") Integer id) {
        return ResponseEntity.ok(tmdbService.getTvWatchProviders(id));
    }

    @GetMapping("{movie}/watch/providers/movie")
    public ResponseEntity<?> getMovieProviders(@PathVariable("movie") Integer movieId) {
        return ResponseEntity.ok(tmdbService.getMovieWatchProviders(movieId));
    }

    @GetMapping("/providers/subscribe")
    public ResponseEntity<List<ProviderDto>> getProviders() {
        return ResponseEntity.ok(tmdbService.getUserProviders());
    }

    @PostMapping("/providers/subscribe")
    public ResponseEntity<String> saveUserProviders(@RequestBody List<ProviderDto> request) {
        tmdbService.saveUserProviders(request);
        return ResponseEntity.ok("Proveedores guardados exitosamente");
    }

    /**
     * Buscar películas o series por título.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMoviesOrSeries(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        var searchResults = tmdbService.search(query, page);
        return ResponseEntity.ok(searchResults);
    }

    @PostMapping("/torrent/{id}/download")
    public ResponseEntity<?> downloadTorrent(@PathVariable Integer id) {
        var torrent = transmissionService.addTorrent(id);
        return ResponseEntity.ok(torrent);
    }

    @GetMapping("/torrent/{id}/status")
    public ResponseEntity<?> checkMovieStatus(@PathVariable Integer id) {
        var status = transmissionService.checkTorrentStatus(id);
        return ResponseEntity.ok(status);
    }
    @GetMapping("/torrent/{id}/{season}/{episode}/status")
    public ResponseEntity<?> checkSeriesStatus(@PathVariable Integer id, @PathVariable Integer season, @PathVariable Integer episode) {
        var status = transmissionService.checkTorrentStatus(id, season, episode);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/torrent/{id}/progress")
    public ResponseEntity<?> checkTorrentProgress(@PathVariable Integer id) {
        TorrentInfo info = transmissionService.checkTorrentProgress(id);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/torrent/{id}/{season}/{episode}/progress")
    public ResponseEntity<?> checkTorrentProgress(@PathVariable Integer id, @PathVariable Integer season, @PathVariable Integer episode) {
        TorrentInfo info = transmissionService.checkTorrentProgress(id, season, episode);
        return ResponseEntity.ok(info);
    }
}
