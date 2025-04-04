package com.stremio.addon.service.transmission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TorrentAddResponse implements ResponseArguments{
    @JsonProperty("torrent-duplicate")
    private TorrentInfo torrentDuplicate;
    @JsonProperty("torrent-added")
    private TorrentInfo torrentAdded;
}
