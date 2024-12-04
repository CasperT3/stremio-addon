package com.stremio.addon.controller.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TorrentSearcher {

    private Long id;

    private String url;

    private String name;

    private String type;

    private String description;
}
