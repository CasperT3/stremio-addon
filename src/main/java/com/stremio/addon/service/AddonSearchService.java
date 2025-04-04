package com.stremio.addon.service;

import com.stremio.addon.controller.dto.Catalog;
import com.stremio.addon.controller.dto.Manifest;
import com.stremio.addon.controller.dto.Stream;
import com.stremio.addon.mapper.TorrentMapper;
import com.stremio.addon.model.SearchModel;
import com.stremio.addon.repository.SearchRepository;
import com.stremio.addon.service.tmdb.TmdbService;
import com.stremio.addon.service.tmdb.dto.FindResults;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AddonSearchService {

    private final TmdbService tmdbService;
    private final SearchService searchService;
    private final SearchRepository searchRepository;

    public List<Stream> searchMoviesTorrent(String id) {
        log.info("Searching movie torrents for ID: {}", id);
        var streams = searchRepository.findByImdbId(id)
                .stream()
                .flatMap(searchModel -> mapToStreams(searchModel).stream())
                .collect(Collectors.toList());

        return streams.isEmpty() ? searchAndSaveTorrent("movie", id) : streams;
    }

    public List<Stream> searchSeriesTorrent(String id, String season, String episode) {
        log.info("Searching series torrents for ID: {}, Season: {}, Episode: {}", id, season, episode);
        return searchRepository.findByImdbIdAndSeasonAndEpisode(id, parseInteger(season), parseInteger(episode))
                .map(this::mapToStreams)
                .orElseGet(() -> searchAndSaveTorrent("series", id, season, episode));
    }

    @Async
    public void fetchAndSaveMovie(String id) {
        log.info("Asynchronously fetching and saving movie with ID: {}", id);
        handleFetchAndSave(id, "movie");
    }

    @Async
    public void fetchAndSaveSeries(String id, String season, String episode) {
        log.info("Asynchronously fetching and saving series with ID: {}, Season: {}, Episode: {}", id, season, episode);
        handleFetchAndSave(id, "series", season, episode);
    }

    private void handleFetchAndSave(String id, String type, String... args) {
        var searchList = searchRepository.findByImdbId(id);
        if (searchList.isEmpty()) {
            CompletableFuture.runAsync(() -> searchAndSaveTorrent(type, id, args));
        } else {
            searchList.stream()
                    .filter(search -> search.getTorrents() == null || search.getTorrents().isEmpty())
                    .forEach(search -> CompletableFuture.runAsync(() -> searchAndSaveTorrent(type, id, args)));
        }
    }

    private List<Stream> searchAndSaveTorrent(String type, String id, String... args) {
        try {
            var result = tmdbService.findById(id, type);
            return "movie".equals(type)
                    ? processMovie(result, id)
                    : processTvShow(result, id, args);
        } catch (Exception e) {
            throw handleException("Error during torrent search and save", e);
        }
    }

    private List<Stream> processMovie(FindResults result, String id) {
        var movie = result.getMovieResults().stream().findFirst()
                .orElseThrow(() -> handleException("Movie not found for ID: " + id, null));
        var title = movie.getTitle();

        return mapToStreams(searchService.searchMovies(id, movie.getId(), title, movie.getReleaseDate()));
    }

    private List<Stream> processTvShow(FindResults result, String id, String... args) {
        var tvShow = result.getTvResults().stream().findFirst()
                .orElseThrow(() -> handleException("TV Show not found for ID: " + id, null));
        var title = tvShow.getName();
        return mapToStreams(searchService.searchSeries(id, tvShow.getId(), title, parseInteger(args[0]), parseInteger(args[1])));
    }

    private List<Stream> mapToStreams(SearchModel searchModel) {
        if (searchModel == null || searchModel.getTorrents() == null) {
            return List.of();
        }
        return searchModel.getTorrents().stream()
                .map(TorrentMapper.INSTANCE::map)
                .collect(Collectors.toList());
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw handleException("Invalid number format: " + value, e);
        }
    }

    private RuntimeException handleException(String message, Exception e) {
        log.error("{} - Cause: {}", message, e != null ? e.getMessage() : "Unknown");
        return new RuntimeException(message, e);
    }

    public void deleteReferences(String id) {
        try {
            log.info("Deleting references for ID {}", id);
            searchRepository.findByImdbId(id)
                    .forEach(searchModel -> searchRepository.deleteById(searchModel.getId()));

            log.info("Successfully deleted references for ID {}", id);
        } catch (Exception e) {
            throw handleException("Error deleting references for ID: " + id, e);
        }
    }

    public Manifest getManifest() {
        log.info("Returning manifest...");
        List<Catalog> catalogs = createCatalogs();
        return Manifest.builder()
                .name("Addon Spanish Torrent")
                .id("com.stremio.addon.torrent.spanish")
                .version("0.0.1")
                .description("Addon Torrent")
                .resources(new String[]{"catalog", "stream"})
                .types(new String[]{"movie", "series"})
                .catalogs(catalogs.toArray(new Catalog[0]))
                .logo("https://upload.wikimedia.org/wikipedia/de/thumb/e/e1/Java-Logo.svg/364px-Java-Logo.svg.png")
                .idPrefixes(new String[]{"tt"})
                .build();
    }

    private List<Catalog> createCatalogs() {
        return List.of(
                Catalog.builder()
                        .id("catalogTorrentMovies")
                        .type("movie")
                        .name("Torrent Movies")
                        .extraRequired(new String[]{})
                        .extraSupported(new String[]{"search"})
                        .build(),
                Catalog.builder()
                        .id("catalogTorrentSeries")
                        .type("series")
                        .name("Torrent Series")
                        .extraRequired(new String[]{})
                        .extraSupported(new String[]{"search"})
                        .build()
        );
    }
}