package com.stremio.addon.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SeasonDetailDto {
    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("episode_count")
    private int episodeCount;

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("season_number")
    private int seasonNumber;

    @JsonProperty("vote_average")
    private double voteAverage;

    private List<EpisodeDetailDto> episodes;
}
