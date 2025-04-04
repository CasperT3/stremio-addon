package com.stremio.addon.service.transmission.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TorrentRemoveArguments implements RequestArguments {
    private List<Integer> ids;
    @JsonProperty("delete-local-data")
    private boolean deleteLocalData;
}
