package com.stremio.addon.service;

import com.stremio.addon.controller.dto.MovieDto;
import com.stremio.addon.controller.dto.SeasonDetailDto;
import com.stremio.addon.controller.dto.SeriesDetailDto;
import com.stremio.addon.controller.dto.SeriesDto;
import com.stremio.addon.service.tmdb.TmdbService;
import com.stremio.addon.service.tmdb.dto.Movie;
import com.stremio.addon.service.tmdb.dto.TvShow;
import com.stremio.addon.service.tmdb.dto.TvShowDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoritesService {

    private final TmdbService tmdbService;
    private final AddonSearchService addonSearchService;

    /**
     * Manage favorite status for a movie or TV show.
     *
     * @param mediaId   The TMDB ID of the movie or TV show.
     * @param mediaType The type of media ("movie" or "tv").
     * @param favorite  Whether to mark as favorite or not.
     */
    public void manageFavorite(long mediaId, String mediaType, boolean favorite) {
        String imdbId = fetchImdbIdentifier(mediaId, mediaType);
        Optional.of(favorite)
                .filter(f -> f)
                .ifPresentOrElse(
                        f -> activateFavorite(imdbId, mediaType, mediaId),
                        () -> deactivateFavorite(imdbId, mediaType, mediaId)
                );
    }

    /**
     * Activate favorite for a movie or TV show.
     */
    private void activateFavorite(String imdbId, String mediaType, long mediaId) {
        log.info("Activating favorite for {} with IMDb ID {}", mediaType, imdbId);

        // Mark as favorite in TMDB
        tmdbService.markAsFavorite(mediaId, mediaType, true);

        // Fetch and save torrent references
        Optional.of(mediaType)
                .filter("movie"::equals)
                .ifPresentOrElse(
                        m -> addonSearchService.fetchAndSaveMovie(imdbId),
                        () -> downloadAllEpisodes(imdbId, mediaId)
                );

        log.info("Successfully activated favorite for {} with IMDb ID {}", mediaType, imdbId);
    }

    /**
     * Deactivate favorite for a movie or TV show.
     */
    private void deactivateFavorite(String imdbId, String mediaType, long mediaId) {
        log.info("Deactivating favorite for {} with IMDb ID {}", mediaType, imdbId);

        // Unmark as favorite in TMDB
        tmdbService.markAsFavorite(mediaId, mediaType, false);

        // Remove torrent references from database
        addonSearchService.deleteReferences(imdbId);

        log.info("Successfully deactivated favorite for {} with IMDb ID {}", mediaType, imdbId);
    }

    /**
     * Download all episodes for a TV show by fetching all seasons and episodes.
     */
    private void downloadAllEpisodes(String imdbId, long mediaId) {
        log.info("Fetching details for TV show with IMDb ID {}", imdbId);

        try {
            Optional.ofNullable(tmdbService.getTvShowDetail((int) mediaId))
                    .map(SeriesDetailDto::getSeasons)
                    .ifPresentOrElse(
                            seasons -> processSeasons(seasons, imdbId),
                            () -> log.warn("No seasons found for TV show with IMDb ID {}", imdbId)
                    );
        } catch (Exception e) {
            log.error("Error fetching details for TV show with IMDb ID {}: {}", imdbId, e.getMessage());
            throw new RuntimeException("Error fetching TV show details: " + e.getMessage(), e);
        }
    }

    /**
     * Process all seasons of a TV show.
     *
     * @param seasons List of seasons of the TV show.
     * @param imdbId  The IMDb ID of the TV show.
     */
    private void processSeasons(List<SeasonDetailDto> seasons, String imdbId) {
        seasons.forEach(season -> {
            int seasonNumber = season.getSeasonNumber();
            log.info("Processing Season {} for TV show with IMDb ID {}", seasonNumber, imdbId);
            processEpisodes(season, imdbId, seasonNumber);
        });
    }

    /**
     * Process all episodes of a season.
     *
     * @param season       The season object containing episode information.
     * @param imdbId       The IMDb ID of the TV show.
     * @param seasonNumber The season number.
     */
    private void processEpisodes(SeasonDetailDto season, String imdbId, int seasonNumber) {
        Optional.of(season.getEpisodeCount())
                .ifPresentOrElse(
                        episodeCount -> saveAllEpisodes(imdbId, seasonNumber, episodeCount),
                        () -> log.warn("No episodes found for Season {} of TV show with IMDb ID {}", seasonNumber, imdbId)
                );
    }

    /**
     * Save all episodes of a season in the database.
     *
     * @param imdbId       The IMDb ID of the TV show.
     * @param seasonNumber The season number.
     * @param episodeCount The total number of episodes in the season.
     */
    private void saveAllEpisodes(String imdbId, int seasonNumber, int episodeCount) {
        for (int episodeNumber = 1; episodeNumber <= episodeCount; episodeNumber++) {
            log.info("Fetching and saving torrent for TV show with IMDb ID {}, Season {}, Episode {}",
                    imdbId, seasonNumber, episodeNumber);
            addonSearchService.fetchAndSaveSeries(
                    imdbId,
                    String.valueOf(seasonNumber),
                    String.valueOf(episodeNumber)
            );
        }
    }

    /**
     * Fetch the IMDb identifier for a given media.
     *
     * @param mediaId   The TMDB ID of the media.
     * @param mediaType The type of the media ("movie" or "tv").
     * @return The corresponding IMDb ID.
     */
    private String fetchImdbIdentifier(long mediaId, String mediaType) {
        log.info("Fetching IMDb identifier for {} with TMDB ID {}", mediaType, mediaId);

        try {
            switch (mediaType) {
                case "movie":
                    var movieDetail = tmdbService.getMovieDetail((int) mediaId);
                    if (movieDetail != null && movieDetail.getImdbId() != null) {
                        log.info("Found IMDb identifier for movie: {}", movieDetail.getImdbId());
                        return movieDetail.getImdbId();
                    }
                    break;
                case "tv":
                    var externalIds = tmdbService.getTvShowExternalIds((int) mediaId);
                    if (externalIds != null && externalIds.get("imdb_id") != null) {
                        String imdbId = (String) externalIds.get("imdb_id");
                        log.info("Found IMDb identifier for TV show: {}", imdbId);
                        return imdbId;
                    }
                    break;
                default:
                    log.error("Unsupported media type: {}", mediaType);
                    throw new IllegalArgumentException("Unsupported media type: " + mediaType);
            }
        } catch (Exception e) {
            log.error("Error fetching IMDb identifier for {} with TMDB ID {}: {}", mediaType, mediaId, e.getMessage());
            throw new RuntimeException("Error fetching IMDb identifier: " + e.getMessage(), e);
        }

        throw new RuntimeException("No IMDb ID found for TMDB ID: " + mediaId);
    }

    /**
     * Scheduled method that verifies and saves favorite movies and TV shows.
     * This method runs every hour to ensure that the favorites are up to date.
     * It processes favorite movies and TV shows by fetching them from the TMDB service
     * based on their popularity.
     */
    
    //@Scheduled(fixedRate = 3600000) // Ejecutar cada hora
    public void verifyAndSaveFavorites() {
        log.info("Starting favorite verification...");

        try {
            // Process favorite movies
            processFavorites(
                    "movie",
                    page -> tmdbService.getFavoriteMovies(page, "popularity.desc").getResults()
            );

            // Process favorite TV shows
            processFavorites(
                    "tv",
                    page -> tmdbService.getFavoriteTvShows(page, "popularity.desc").getResults()
            );

            log.info("Favorite verification completed successfully.");
        } catch (Exception e) {
            log.error("Error during favorite verification: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes and manages favorite media items (movies or TV shows).
     * This method fetches paginated results using the provided fetch function
     * and manages each favorite item by calling the manageFavorite method.
     *
     * @param mediaType     the type of media being processed (e.g., "movie" or "tv")
     * @param fetchFunction a function that takes a page number and returns a list of media items
     * @param <T>           the type of media items (either Movie or TvShow)
     */
    private <T> void processFavorites(String mediaType, Function<Integer, List<T>> fetchFunction) {
        int page = 1;
        while (true) {
            List<T> results = fetchFunction.apply(page);
            if (results.isEmpty()) break;

            results.forEach(item -> {
                long mediaId;
                if (item instanceof MovieDto movie) {
                    mediaId = movie.getId();
                } else if (item instanceof SeriesDto tvShow) {
                    mediaId = tvShow.getId();
                } else {
                    throw new IllegalArgumentException("Unsupported media type");
                }

                manageFavorite(mediaId, mediaType, true);
            });

            if (page >= results.size()) break; // If the total number of pages is reached, stop
            page++;
        }
    }

}