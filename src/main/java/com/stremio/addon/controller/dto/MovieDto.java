package com.stremio.addon.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MovieDto {
    @JsonProperty("id")
    private int id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("media_type")
    private String mediaType;

    @JsonProperty("adult")
    private boolean adult;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("genres")
    private List<String> genres;

    @JsonProperty("popularity")
    private double popularity;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("video")
    private boolean video;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("vote_count")
    private int voteCount;
}
