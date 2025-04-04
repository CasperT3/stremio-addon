package com.stremio.addon.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SeriesDetailDto {
    @JsonProperty("adult")
    private boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("episode_run_time")
    private List<Integer> episodeRunTime;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    @JsonProperty("genres")
    private List<String> genres;

    @JsonProperty("homepage")
    private String homepage;

    @JsonProperty("id")
    private int id;

    @JsonProperty("in_production")
    private boolean inProduction;

    @JsonProperty("languages")
    private List<String> languages;

    @JsonProperty("last_air_date")
    private String lastAirDate;

    @JsonProperty("last_episode_to_air")
    private EpisodeDetailDto lastEpisodeToAir;

    @JsonProperty("name")
    private String name;

    @JsonProperty("next_episode_to_air")
    private EpisodeDetailDto nextEpisodeToAir;

    @JsonProperty("number_of_episodes")
    private int numberOfEpisodes;

    @JsonProperty("number_of_seasons")
    private int numberOfSeasons;

    @JsonProperty("origin_country")
    private List<String> originCountry;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_name")
    private String originalName;

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("popularity")
    private double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("type")
    private String type;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("vote_count")
    private int voteCount;

    private List<SeasonDetailDto> seasons;
}
