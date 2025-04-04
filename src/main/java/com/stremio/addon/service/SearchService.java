package com.stremio.addon.service;

import com.stremio.addon.mapper.TorrentMapper;
import com.stremio.addon.model.SearchEngineModel;
import com.stremio.addon.model.SearchModel;
import com.stremio.addon.model.TorrentInfoModel;
import com.stremio.addon.repository.SearchEngineRepository;
import com.stremio.addon.repository.SearchRepository;
import com.stremio.addon.service.searcher.TorrentSearcherFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SearchService {

    private final SearchEngineRepository searchEngineRepository;
    private final TorrentSearcherFactory searcherFactory;
    private final SearchRepository searchRepository;
    private final TorrentDownloaderService torrentDownloaderService;

    public SearchService(SearchEngineRepository searchEngineRepository, TorrentSearcherFactory searcherFactory, SearchRepository searchRepository, TorrentDownloaderService torrentDownloaderService) {
        this.searchEngineRepository = searchEngineRepository;
        this.searcherFactory = searcherFactory;
        this.searchRepository = searchRepository;
        this.torrentDownloaderService = torrentDownloaderService;
    }

    public SearchModel searchMovies(String id, int tmdbId, String title, String date) {
        var year = extractYear(date);
        var torrents = searchTorrent("movie", id, title, year);

        if (torrents.isEmpty()) {
            log.info("No torrents found for movie: [{}] [{}]", id, title);
            torrents = List.of();
        }
        return saveSearch(id, tmdbId, title, "movie", torrents, Integer.parseInt(year), null, null);
    }

    public SearchModel searchSeries(String id, int tmdbId, String title, Integer season, Integer episode) {
        var torrents = searchTorrent("series", id, title, String.valueOf(season), String.valueOf(episode));

        if (torrents.isEmpty()) {
            log.info("No torrents found for series: [{}] [{}] [{}] [{}]", id, title, season, episode);
            torrents = List.of();
        }
        return saveSearch(id, tmdbId, title, "series", torrents, null, season, episode);
    }

    private List<String> searchTorrent(String type, String id, String title, String... args) {
        log.info("Searching torrents for type [{}] with ID [{}] and args [{}]", type, id, args);
        return searchEngineRepository.findByActive(true).parallelStream()
                .peek(provider -> log.info("Searching in provider: {}", provider.getName()))
                .flatMap(provider -> searchTorrentByProvider(title, args, type, provider).stream())
                .collect(Collectors.toList());
    }

    private List<String> searchTorrentByProvider(String title, String[] args, String type, SearchEngineModel provider) {
        try {
            var searcher = searcherFactory.getSearcher(type, provider);
            return searcher.searchTorrents(title, args);
        } catch (Exception e) {
            log.error("Error searching torrents with provider {}: {}", provider.getName(), e.getMessage());
            return List.of();
        }
    }

    private SearchModel saveSearch(String id, int tmdbId, String title, String type, List<String> torrents, Integer year, Integer season, Integer episode) {
        log.info("Saving search for {}: [{}] [{}]", type, id, title);

        return searchRepository.findByImdbIdAndSeasonAndEpisode(id, season, episode)
                .map(search -> {
                    search.setTorrents(mapToSetTorrents(torrents));
                    return searchRepository.save(search);
                }).orElseGet(() -> {
                    var newSearch = SearchModel.builder()
                            .imdbId(id)
                            .tmdbId(tmdbId)
                            .type(type)
                            .title(title)
                            .year(year)
                            .season(season)
                            .episode(episode)
                            .torrents(mapToSetTorrents(torrents))
                            .searchTime(LocalDateTime.now())
                            .build();
                    return searchRepository.save(newSearch);
                });
    }

    private Set<TorrentInfoModel> mapToSetTorrents(List<String> torrents) {
        return torrents.stream()
                .map(torrentDownloaderService::downloadTorrent)
                .filter(bytes -> bytes != null && bytes.length > 0)
                .map(TorrentMapper.INSTANCE::map)
                .collect(Collectors.toSet());
    }

    private RuntimeException handleException(String message, Exception e) {
        log.error("{} - Cause: {}", message, e != null ? e.getMessage() : "Unknown");
        return new RuntimeException(message, e);
    }

    private String extractYear(String date) {
        return date != null && date.length() >= 4 ? date.substring(0, 4) : "N/A";
    }

}
