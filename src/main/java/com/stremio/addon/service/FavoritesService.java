package com.stremio.addon.service;

import com.stremio.addon.service.tmdb.TmdbService;
import com.stremio.addon.service.tmdb.dto.TvShowDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

        Optional.ofNullable(tmdbService.getTvShowDetail((int) mediaId))
                .map(TvShowDetail::getSeasons)
                .ifPresentOrElse(
                        seasons -> processSeasons(seasons, imdbId),
                        () -> log.warn("No seasons found for TV show with IMDb ID {}", imdbId)
                );
    }

    /**
     * Process all seasons of a TV show.
     *
     * @param seasons List of seasons of the TV show.
     * @param imdbId  The IMDb ID of the TV show.
     */
    private void processSeasons(List<TvShowDetail.Season> seasons, String imdbId) {
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
    private void processEpisodes(TvShowDetail.Season season, String imdbId, int seasonNumber) {
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
            if ("movie".equals(mediaType)) {
                var movieDetail = tmdbService.getMovieDetail((int) mediaId);
                if (movieDetail != null && movieDetail.getImdbId() != null) {
                    log.info("Found IMDb identifier for movie: {}", movieDetail.getImdbId());
                    return movieDetail.getImdbId();
                }
            } else if ("tv".equals(mediaType)) {
                var externalIds = tmdbService.getTvShowExternalIds((int) mediaId);
                if (externalIds != null && externalIds.get("imdb_id") != null) {
                    String imdbId = (String) externalIds.get("imdb_id");
                    log.info("Found IMDb identifier for TV show: {}", imdbId);
                    return imdbId;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching IMDb identifier for {} with TMDB ID {}: {}", mediaType, mediaId, e.getMessage());
        }

        throw new RuntimeException("No IMDb ID found for TMDB ID: " + mediaId);
    }
}
