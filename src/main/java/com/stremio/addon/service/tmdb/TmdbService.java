package com.stremio.addon.service.tmdb;

import com.stremio.addon.configuration.TmdbConfiguration;
import com.stremio.addon.controller.dto.*;
import com.stremio.addon.mapper.MovieMapper;
import com.stremio.addon.mapper.ProviderMapper;
import com.stremio.addon.mapper.SerieMapper;
import com.stremio.addon.mapper.TorrentMapper;
import com.stremio.addon.model.ProviderModel;
import com.stremio.addon.repository.ProviderRepository;
import com.stremio.addon.repository.SearchRepository;
import com.stremio.addon.service.SearchService;
import com.stremio.addon.service.tmdb.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class TmdbService {

    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of("movie", "movie_results", "series", "tv_results");
    private static final Map<String, String> TITLE_KEY_MAP = Map.of("movie", "title", "series", "name");
    private final TmdbConfiguration configuration;
    private final RestTemplate restTemplate;
    private final ProviderRepository providerRepository;
    private final SearchRepository searchRepository;
    private final SearchService searchService;

    @Autowired
    public TmdbService(TmdbConfiguration configuration, @Qualifier("restTemplateJson") RestTemplate restTemplate, ProviderRepository providerRepository, SearchRepository searchRepository, SearchService searchService) {
        this.configuration = configuration;
        this.restTemplate = restTemplate;
        this.providerRepository = providerRepository;
        this.searchRepository = searchRepository;
        this.searchService = searchService;
    }

    /**
     * Retrieve the name of a movie or TV show from TMDB based on the IMDb ID.
     */
    public String getTitle(String imdbId, String contentType) {
        String url = String.format("%s/find/%s?external_source=imdb_id&language=es-ES", configuration.getApiUrl(), imdbId);
        logRequest("Get Title", url, Map.of("IMDb ID", imdbId, "Content Type", contentType));
        return executeRequest(url, Map.class, response -> extractTitle(response, contentType, imdbId));
    }

    /**
     * Buscar información por ID en TMDB.
     * <p>
     * Este método utiliza la API de TMDB para buscar información sobre películas, series o personas
     * usando un ID específico y un origen externo.
     *
     * @param id             El ID a buscar.
     * @param externalSource El origen externo (por ejemplo, "imdb_id", "tvdb_id").
     * @return Un objeto Map que contiene los resultados.
     */
    public FindResults findById(String id, String externalSource) {
        String url = String.format("%s/find/%s?external_source=imdb_id&language=es-ES", configuration.getApiUrl(), id, externalSource);
        logRequest("Find By ID", url, Map.of("ID", id, "External Source", externalSource));
        return executeRequest(url, FindResults.class);
    }


    public Map<?, ?> getTvShowExternalIds(int tmdbId) {
        String url = String.format("%s/tv/%d/external_ids", configuration.getApiUrl(), tmdbId);
        return executeRequest(url, Map.class);
    }

    /**
     * Retrieve detailed information of a movie from TMDB based on the TMDB ID.
     */
    public MovieDetailDto getMovieDetail(int tmdbId) {
        String url = String.format("%s/movie/%d?language=es-ES", configuration.getApiUrl(), tmdbId);
        logRequest("Get Movie Detail", url, Map.of("TMDB ID", tmdbId));
        return MovieMapper.INSTANCE.map(executeRequest(url, MovieDetail.class));
    }

    /**
     * Retrieve detailed information of a TV show from TMDB based on the TMDB ID.
     */
    public SeriesDetailDto getTvShowDetail(int tmdbId) {
        String url = String.format("%s/tv/%d?language=es-ES", configuration.getApiUrl(), tmdbId);
        logRequest("Get TV Show Detail", url, Map.of("TMDB ID", tmdbId));
        return SerieMapper.INSTANCE.map(executeRequest(url, TvShowDetail.class));
    }

    /**
     * Fetch trending movies for the day with pagination.
     */
    public ResultsDto<MovieDto> getTrendingMovies(int page) {
        String url = String.format("%s/trending/movie/day?language=es-ES&page=%d", configuration.getApiUrl(), page);
        logRequest("Get Trending Movies", url, Map.of("Page", page));
        return MovieMapper.INSTANCE.map(executeRequest(url, PaginatedMovies.class));
    }

    /**
     * Fetch trending TV shows for the day with pagination.
     */
    public ResultsDto<SeriesDto> getTrendingTvShows(int page) {
        String url = String.format("%s/trending/tv/day?language=es-ES&page=%d", configuration.getApiUrl(), page);
        logRequest("Get Trending TV Shows", url, Map.of("Page", page));
        return SerieMapper.INSTANCE.map(executeRequest(url, PaginatedTvShows.class));
    }

    /**
     * Fetch favorite movies for the current account with pagination and sorting.
     */
    public ResultsDto<MovieDto> getFavoriteMovies(int page, String sortBy) {
        String url = String.format("%s/account/%s/favorite/movies?language=es-ES&page=%d&sort_by=%s", configuration.getApiUrl(), configuration.getAccountId(), page, sortBy);
        logRequest("Get Favorite Movies", url, Map.of("Page", page, "Sort By", sortBy));
        return MovieMapper.INSTANCE.map(executeRequest(url, PaginatedMovies.class));
    }

    /**
     * Fetch favorite TV shows for the current account with pagination and sorting.
     */
    public ResultsDto<SeriesDto> getFavoriteTvShows(int page, String sortBy) {
        String url = String.format("%s/account/%s/favorite/tv?language=es-ES&page=%d&sort_by=%s", configuration.getApiUrl(), configuration.getAccountId(), page, sortBy);
        logRequest("Get Favorite TV Shows", url, Map.of("Page", page, "Sort By", sortBy));
        return SerieMapper.INSTANCE.map(executeRequest(url, PaginatedTvShows.class));
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
     * Retrieve episode details for a specific season of a TV show.
     *
     * @param tvShowId     The TMDB ID of the TV show.
     * @param seasonNumber The season number to fetch.
     * @return A list of episode details.
     */
    public SeasonDetailDto getSeasonDetails(int tvShowId, int seasonNumber) {
        String url = String.format("%s/tv/%d/season/%d?language=es-ES", configuration.getApiUrl(), tvShowId, seasonNumber);
        logRequest("Get Season Details", url, Map.of("TV Show ID", tvShowId, "Season Number", seasonNumber));
        return SerieMapper.INSTANCE.map(executeRequest(url, SeasonDetail.class));
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
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, createHttpEntity(), responseType);
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
     * Discover movies with advanced search filters.
     *
     * @param params A map of query parameters (e.g., "release_date.gte", "genres", etc.)
     * @return A paginated list of movies.
     */
    public ResultsDto<MovieDto> discoverMovies(Map<String, String> params) {
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/discover/movie?language=es-ES", configuration.getApiUrl()));
        params.forEach((key, value) -> urlBuilder.append("&").append(key).append("=").append(value));
        String url = urlBuilder.toString();
        return MovieMapper.INSTANCE.map(executeRequest(url, PaginatedMovies.class));
    }

    /**
     * Discover TV shows with advanced search filters.
     *
     * @param params A map of query parameters (e.g., "first_air_date.gte", "genres", etc.)
     * @return A paginated list of TV shows.
     */
    public ResultsDto<SeriesDto> discoverTvShows(Map<String, String> params) {
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/discover/tv?language=es-ES", configuration.getApiUrl()));
        params.forEach((key, value) -> urlBuilder.append("&").append(key).append("=").append(value));
        String url = urlBuilder.toString();
        //    logRequest("Discover TV Shows", url, params);
        return SerieMapper.INSTANCE.map(executeRequest(url, PaginatedTvShows.class));
    }

    /**
     * Fetch watch providers for TV shows.
     * Retrieves the available streaming providers for TV content.
     *
     * @return A response object containing the list of providers.
     */
    public List<ProviderDto> getTvWatchProviders() {
        String url = String.format("%s/watch/providers/tv?language=es-ES&watch_region=ES", configuration.getApiUrl());
        logRequest("Get TV Watch Providers", url, Map.of());
        return ProviderMapper.INSTANCE.map(executeRequest(url, ProvidersResponse.class).getResults());
    }

    /**
     * Fetch watch providers for Movies.
     * Retrieves the available streaming providers for Movie content.
     *
     * @return A response object containing the list of providers.
     */
    public List<ProviderDto> getMovieProviders() {
        String url = String.format("%s/watch/providers/movie?language=es-ES&watch_region=ES", configuration.getApiUrl());
        logRequest("Get TV Watch Providers", url, Map.of());
        return ProviderMapper.INSTANCE.map(executeRequest(url, ProvidersResponse.class).getResults());
    }

    public void saveUserProviders(List<ProviderDto> providers) {
        Set<ProviderModel> existingProviders = StreamSupport.stream(providerRepository.findAll().spliterator(), false).collect(Collectors.toSet());

        Set<ProviderModel> newProviders = providers.stream().map(ProviderMapper.INSTANCE::map).collect(Collectors.toSet());

        // Filtrar los proveedores que no están en la base de datos y deben ser guardados
        Set<ProviderModel> toAdd = newProviders.stream().filter(provider -> !contains(existingProviders, provider)).collect(Collectors.toSet());

        // Filtrar los proveedores que están en la base de datos pero no en el nuevo listado y deben ser eliminados
        Set<ProviderModel> toRemove = existingProviders.stream().filter(provider -> !contains(newProviders, provider)).collect(Collectors.toSet());

        // Guardar los nuevos proveedores
        if (!toAdd.isEmpty()) {
            providerRepository.saveAll(toAdd);
            log.info("Providers added: {}", toAdd);
        }

        // Eliminar los proveedores obsoletos
        if (!toRemove.isEmpty()) {
            providerRepository.deleteAll(toRemove);
            log.info("Providers removed: {}", toRemove);
        }
    }

    private boolean contains(Set<ProviderModel> providers, ProviderModel provider) {
        return !providers.stream().filter(providerModel -> providerModel.getProviderId().equals(provider.getProviderId())).toList().isEmpty();
    }

    public List<ProviderDto> getUserProviders() {
        return StreamSupport.stream(providerRepository.findAll().spliterator(), false).map(ProviderMapper.INSTANCE::map).collect(Collectors.toList());
    }

    /**
     * Fetch watch providers by Movie ID.
     *
     * @param movieId The ID of the movie.
     * @return A map containing the watch providers for the movie.
     */
    public WatchProviders getWatchProvidersByMovieId(int movieId) {
        String url = String.format("%s/movie/%d/watch/providers", configuration.getApiUrl(), movieId);
        logRequest("Get Watch Providers", url, Map.of("Movie ID", movieId));
        return executeRequest(url, WatchProviders.class);
    }

    /**
     * Fetch watch providers by Series ID.
     *
     * @param seriesId The ID of the movie.
     * @return A map containing the watch providers for the movie.
     */
    public WatchProviders getWatchProvidersBySeriesId(int seriesId) {
        String url = String.format("%s/tv/%d/watch/providers", configuration.getApiUrl(), seriesId);
        logRequest("Get Watch Providers", url, Map.of("Series ID", seriesId));
        return executeRequest(url, WatchProviders.class);
    }

    public List<ProviderDto> getTvWatchProviders(Integer id) {
        WatchProviders watchProviders = getWatchProvidersBySeriesId(id);
        return getProviders(watchProviders);
    }

    public List<ProviderDto> getMovieWatchProviders(Integer id) {
        WatchProviders watchProviders = getWatchProvidersByMovieId(id);
        return getProviders(watchProviders);
    }

    private List<ProviderDto> getProviders(WatchProviders watchProviders) {
        if (watchProviders.getResults().isEmpty()) return List.of();
        else {
            var providers = watchProviders.getResults().get("ES");
            if (providers == null) return List.of();
            else {
                var result = ProviderMapper.INSTANCE.map(providers.getFlatrate());
                return result == null ? List.of() : result;
            }
        }
    }

    /**
     * Buscar películas o series por título.
     *
     * @param query Término de búsqueda.
     * @param page  Número de página para la paginación.
     * @return Resultados paginados de la búsqueda.
     */
    public PaginatedSearchResults search(String query, int page) {
        String url = String.format("%s/search/multi?query=%s&language=es-ES&page=%d", configuration.getApiUrl(), query, page);
        logRequest("Search Movies/Series", url, Map.of("Query", query, "Page", page));
        return executeRequest(url, PaginatedSearchResults.class);
    }

    public List<TorrentDto> getTorrentsForMovie(int movieId) {
        return searchRepository.findByTmdbId(movieId)
                .stream().findFirst()
                .map(searchModel -> {
                    return searchModel.getTorrents().stream().map(TorrentMapper.INSTANCE::mapToDto).collect(Collectors.toList());
                }).orElseGet(() -> searchTorrents(movieId, "movie"));
    }

    public List<TorrentDto> getTorrentsForTvShow(int tvShowId, int season, int episode) {
        return searchRepository.findByTmdbIdAndSeasonAndEpisode(tvShowId, season, episode)
                .map(searchModel -> {
                    return searchModel.getTorrents().stream().map(TorrentMapper.INSTANCE::mapToDto).collect(Collectors.toList());
                }).orElseGet(() -> searchTorrents(tvShowId, "tv",  season, episode));
    }

    private @NotNull List<TorrentDto> searchTorrents(int id, String type, int ... args) {
        return "movie".equals(type)
                ? processMovie(id)
                : processTvShow(id, args);
    }

    private List<TorrentDto> processTvShow(int id, int[] args) {
        var tvShow = getTvShowDetail(id);
        var externalIds = getTvShowExternalIds(id);
        var title = tvShow.getName();
        return searchService.searchSeries((String) externalIds.get("imdb_id"), tvShow.getId(), title, args[0], args[1])
                .getTorrents()
                .stream().map(TorrentMapper.INSTANCE::mapToDto)
                .collect(Collectors.toList());
    }

    private List<TorrentDto> processMovie(int id) {
        var movie = getMovieDetail(id);
        var title = movie.getTitle();
        return searchService.searchMovies(movie.getImdbId(), movie.getId(), title, movie.getReleaseDate())
                .getTorrents()
                .stream().map(TorrentMapper.INSTANCE::mapToDto)
                .collect(Collectors.toList());
    }

    @FunctionalInterface
    private interface ResponseProcessor<T, R> {
        R process(T response);
    }
}
