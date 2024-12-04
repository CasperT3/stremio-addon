package com.stremio.addon.service.tmdb;

import com.stremio.addon.configuration.TmdbConfiguration;
import com.stremio.addon.service.tmdb.dto.MovieDetail;
import com.stremio.addon.service.tmdb.dto.PaginatedMovies;
import com.stremio.addon.service.tmdb.dto.PaginatedTvShows;
import com.stremio.addon.service.tmdb.dto.TvShowDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TmdbService {

    private final TmdbConfiguration configuration;
    private final RestTemplate restTemplate;

    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "movie", "movie_results",
            "series", "tv_results"
    );

    private static final Map<String, String> TITLE_KEY_MAP = Map.of(
            "movie", "title",
            "series", "name"
    );

    @Autowired
    public TmdbService(TmdbConfiguration configuration, @Qualifier("restTemplateJson") RestTemplate restTemplate) {
        this.configuration = configuration;
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieve the name of a movie or TV show from TMDB based on the IMDb ID.
     */
    public String getTitle(String imdbId, String contentType) {
        String url = String.format("%s/find/%s?external_source=imdb_id&language=es-ES", configuration.getApiUrl(), imdbId);
        logRequest("Get Title", url, Map.of("IMDb ID", imdbId, "Content Type", contentType));
        return executeRequest(url, Map.class, response -> extractTitle(response, contentType, imdbId));
    }

    public Map<?, ?> getTvShowExternalIds(int tmdbId) {
        String url = String.format("%s/tv/%d/external_ids", configuration.getApiUrl(), tmdbId);
        return executeRequest(url, Map.class);
    }

    /**
     * Retrieve detailed information of a movie from TMDB based on the TMDB ID.
     */
    public MovieDetail getMovieDetail(int tmdbId) {
        String url = String.format("%s/movie/%d?language=es-ES", configuration.getApiUrl(), tmdbId);
        logRequest("Get Movie Detail", url, Map.of("TMDB ID", tmdbId));
        return executeRequest(url, MovieDetail.class);
    }

    /**
     * Retrieve detailed information of a TV show from TMDB based on the TMDB ID.
     */
    public TvShowDetail getTvShowDetail(int tmdbId) {
        String url = String.format("%s/tv/%d?language=es-ES", configuration.getApiUrl(), tmdbId);
        logRequest("Get TV Show Detail", url, Map.of("TMDB ID", tmdbId));
        return executeRequest(url, TvShowDetail.class);
    }

    /**
     * Fetch trending movies for the day with pagination.
     */
    public PaginatedMovies getTrendingMovies(int page) {
        String url = String.format("%s/trending/movie/day?language=es-ES&page=%d", configuration.getApiUrl(), page);
        logRequest("Get Trending Movies", url, Map.of("Page", page));
        return executeRequest(url, PaginatedMovies.class);
    }

    /**
     * Fetch trending TV shows for the day with pagination.
     */
    public PaginatedTvShows getTrendingTvShows(int page) {
        String url = String.format("%s/trending/tv/day?language=es-ES&page=%d", configuration.getApiUrl(), page);
        logRequest("Get Trending TV Shows", url, Map.of("Page", page));
        return executeRequest(url, PaginatedTvShows.class);
    }

    /**
     * Fetch favorite movies for the current account with pagination and sorting.
     */
    public PaginatedMovies getFavoriteMovies(int page, String sortBy) {
        String url = String.format("%s/account/%s/favorite/movies?language=es-ES&page=%d&sort_by=%s",
                configuration.getApiUrl(), configuration.getAccountId(), page, sortBy);
        logRequest("Get Favorite Movies", url, Map.of("Page", page, "Sort By", sortBy));
        return executeRequest(url, PaginatedMovies.class);
    }

    /**
     * Fetch favorite TV shows for the current account with pagination and sorting.
     */
    public PaginatedTvShows getFavoriteTvShows(int page, String sortBy) {
        String url = String.format("%s/account/%s/favorite/tv?language=es-ES&page=%d&sort_by=%s",
                configuration.getApiUrl(), configuration.getAccountId(), page, sortBy);
        logRequest("Get Favorite TV Shows", url, Map.of("Page", page, "Sort By", sortBy));
        return executeRequest(url, PaginatedTvShows.class);
    }

    /**
     * Mark or unmark a movie or TV show as a favorite for the current account.
     */
    public void markAsFavorite(long mediaId, String mediaType, boolean favorite) {
        String url = String.format("%s/account/%s/favorite", configuration.getApiUrl(), configuration.getAccountId());
        String payload = String.format("{\"media_type\": \"%s\", \"media_id\": %d, \"favorite\": %b}", mediaType, mediaId, favorite);
        logRequest("Mark As Favorite", url, Map.of("Media ID", mediaId, "Media Type", mediaType, "Favorite", favorite));
        executePostRequest(url, payload);
    }

    /**
     * General method to execute GET requests.
     */
    private <T> T executeRequest(String url, Class<T> responseType) {
        return executeRequest(url, responseType, response -> response);
    }

    /**
     * General method to execute GET requests with custom response processing.
     */
    private <T, R> R executeRequest(String url, Class<T> responseType, ResponseProcessor<T, R> processor) {
        try {
            log.debug("Executing GET request: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createHttpEntity(),
                    responseType
            );
            log.info("Successfully executed GET request: {}", url);
            return processor.process(response.getBody());
        } catch (Exception e) {
            throw handleException(e, "Error during GET request to URL: " + url);
        }
    }

    /**
     * General method to execute POST requests.
     */
    private void executePostRequest(String url, String payload) {
        try {
            log.debug("Executing POST request: {} with payload: {}", url, payload);
            restTemplate.postForEntity(url, createHttpEntity(payload), String.class);
            log.info("Successfully executed POST request: {}", url);
        } catch (Exception e) {
            throw handleException(e, "Error during POST request to URL: " + url);
        }
    }

    /**
     * Handle exceptions centrally.
     */
    private RuntimeException handleException(Exception e, String contextMessage) {
        if (e instanceof HttpClientErrorException || e instanceof HttpServerErrorException) {
            log.error("{} - HTTP error: {}", contextMessage, e.getMessage(), e);
            return new RuntimeException("HTTP error: " + e.getMessage(), e);
        }
        log.error("{} - Unexpected error: {}", contextMessage, e.getMessage(), e);
        return new RuntimeException("Unexpected error: " + e.getMessage(), e);
    }

    /**
     * Log request information in a structured format.
     */
    private void logRequest(String operation, String url, Map<String, Object> parameters) {
        log.info("Operation: {}", operation);
        log.info("Request URL: {}", url);
        parameters.forEach((key, value) -> log.info("Parameter - {}: {}", key, value));
    }

    /**
     * Create an HttpEntity with default headers.
     */
    private HttpEntity<String> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + configuration.getApiKey());
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    /**
     * Create an HttpEntity with default headers and a JSON payload.
     */
    private HttpEntity<String> createHttpEntity(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + configuration.getApiKey());
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        return new HttpEntity<>(payload, headers);
    }

    /**
     * Extract title from a response map.
     */
    private String extractTitle(Map<String, Object> responseBody, String contentType, String imdbId) {
        String resultKey = CONTENT_TYPE_MAP.get(contentType);
        if (responseBody == null || resultKey == null || !responseBody.containsKey(resultKey)) {
            throw new RuntimeException("No results found for IMDb ID: " + imdbId);
        }
        var results = (List<Map<String, Object>>) responseBody.get(resultKey);
        if (results.isEmpty()) {
            throw new RuntimeException("No results found for IMDb ID: " + imdbId);
        }
        return (String) results.get(0).get(TITLE_KEY_MAP.get(contentType));
    }

    /**
     * Functional interface for processing responses.
     */
    @FunctionalInterface
    private interface ResponseProcessor<T, R> {
        R process(T response);
    }
}
