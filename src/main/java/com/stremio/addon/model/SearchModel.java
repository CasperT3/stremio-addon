package com.stremio.addon.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Table("search")
@Builder
public class SearchModel {

    @Id
    private Integer id;

    @Column("type")
    private String type; // Representa el ENUM ('series', 'movies') como String en Java.

    @Column("imdb_id")
    private String imdbId;

    @Column("tmdb_id")
    private Integer tmdbId;

    @Column("title")
    private String title;

    @Column("year")
    private Integer year;

    @Column("season")
    private Integer season;

    @Column("episode")
    private Integer episode;

    @Column("search_time")
    private LocalDateTime searchTime; // Mapeo para DATETIME como LocalDateTime.

    @MappedCollection(idColumn = "search_id")
    private Set<TorrentInfoModel> torrents;
}
