package com.stremio.addon.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SeriesDto {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("original_name")
    private String originalName;

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

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    @JsonProperty("popularity")
    private double popularity;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("vote_count")
    private int voteCount;

    @JsonProperty("origin_country")
    private List<String> originCountry;
}

