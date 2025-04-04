package com.stremio.addon.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SearchDto {
    private Integer id;
    private String mediaType; // "movie" o "tv"
    private String title;
    private String name;
    private String overview;
    @JsonProperty("release_date")
    private String releaseDate;
    @JsonProperty("first_air_date")
    private String firstAirDate;
    @JsonProperty("poster_path")
    private String posterPath;
}
