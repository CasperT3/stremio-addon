package com.stremio.addon.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TorrentDto {
    private Integer id;        // ID del torrent
    private String name;       // Nombre del torrent
    private String size;       // Tamaño del archivo (por ejemplo, "1.2 GB")
    private String url;        // URL para descargar el torrent
    private Integer downloadId;
}
