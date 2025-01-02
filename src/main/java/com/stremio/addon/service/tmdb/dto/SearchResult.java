package com.stremio.addon.service.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SearchResult {
    private Integer id;
    private String mediaType; // "movie" o "tv"
    private String title;
    private String name;
    private String overview;
    private String releaseDate;
    private String firstAirDate;
    private String posterPath;

    // Mapear los nombres alternativos para consistencia
    @JsonProperty("release_date")
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @JsonProperty("first_air_date")
    public void setFirstAirDate(String firstAirDate) {
        this.firstAirDate = firstAirDate;
    }

    @JsonProperty("poster_path")
    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }
}
