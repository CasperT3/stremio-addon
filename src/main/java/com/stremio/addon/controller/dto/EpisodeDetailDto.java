package com.stremio.addon.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EpisodeDetailDto {
    @JsonProperty("air_date")
    private String airDate;
    @JsonProperty("episode_number")
    private int episodeNumber;
    private String name;
    private String overview;
    private int runtime;
    @JsonProperty("vote_average")
    private double voteAverage;
    @JsonProperty("vote_count")
    private int voteCount;
    @JsonProperty("still_path")
    private String stillPath;
}
