package com.stremio.addon.service.transmission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TorrentRemoveResponse implements ResponseArguments {
    private List<Integer> removed;
}
