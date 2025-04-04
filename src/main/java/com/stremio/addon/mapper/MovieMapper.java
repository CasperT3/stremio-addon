package com.stremio.addon.mapper;

import com.stremio.addon.controller.dto.MovieDetailDto;
import com.stremio.addon.controller.dto.MovieDto;
import com.stremio.addon.controller.dto.ResultsDto;
import com.stremio.addon.service.tmdb.dto.Movie;
import com.stremio.addon.service.tmdb.dto.MovieDetail;
import com.stremio.addon.service.tmdb.dto.PaginatedMovies;
import com.stremio.addon.service.tmdb.dto.TvShowDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface MovieMapper {
    MovieMapper INSTANCE = Mappers.getMapper(MovieMapper.class);

    MovieDto map(Movie movie);

    ResultsDto<MovieDto> map(PaginatedMovies paginatedMovies);

    @Mapping(source = "genres", target = "genres", qualifiedByName = "getGenres")
    MovieDetailDto map(MovieDetail movieDetail);

    @Named("getGenres")
    default List<String> getGenres(List<MovieDetail.Genre> genres) {
        return genres.stream()
                .map(MovieDetail.Genre::getName)
                .collect(Collectors.toList());
    }
}
