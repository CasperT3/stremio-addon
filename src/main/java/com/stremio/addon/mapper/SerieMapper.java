package com.stremio.addon.mapper;

import com.stremio.addon.controller.dto.ResultsDto;
import com.stremio.addon.controller.dto.SeasonDetailDto;
import com.stremio.addon.controller.dto.SeriesDetailDto;
import com.stremio.addon.controller.dto.SeriesDto;
import com.stremio.addon.service.tmdb.dto.PaginatedTvShows;
import com.stremio.addon.service.tmdb.dto.SeasonDetail;
import com.stremio.addon.service.tmdb.dto.TvShow;
import com.stremio.addon.service.tmdb.dto.TvShowDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface SerieMapper {
    SerieMapper INSTANCE = Mappers.getMapper(SerieMapper.class);

    SeriesDto map(TvShow tvShow);

    ResultsDto<SeriesDto> map(PaginatedTvShows paginatedTvShows);

    SeasonDetailDto map(SeasonDetail seasonDetails);

    @Mapping(source = "genres", target = "genres", qualifiedByName = "getGenres")
    SeriesDetailDto map(TvShowDetail tvShowDetail);

    @Named("getGenres")
    default List<String> getGenres(List<TvShowDetail.Genre> genres) {
        return genres.stream()
                .map(TvShowDetail.Genre::getName)
                .collect(Collectors.toList());
    }
}
