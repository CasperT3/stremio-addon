package com.stremio.addon.repository;

import com.stremio.addon.model.SearchModel;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public interface SearchRepository extends CrudRepository<SearchModel, Integer> {
    List<SearchModel> findByImdbId(String id);

    Optional<SearchModel> findByImdbIdAndSeasonAndEpisode(String imdbId, Integer season, Integer episode);

    List<SearchModel> findByTmdbId(int id);

    Optional<SearchModel> findByTmdbIdAndSeasonAndEpisode(int tvShowId, int season, int episode);
}

