package com.stremio.addon.service.transmission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionSetArguments implements RequestArguments{
    @JsonProperty("script-torrent-done-filename")
    private String scriptTorrentDoneFilename;
}
