package com.stremio.addon.service.transmission.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TorrentInfo {
    private int id;
    private String name;
    private String hashString;
    private double percentDone;
    private long totalSize;
    @JsonDeserialize(using = TorrentStatusDeserializer.class)
    private TorrentStatus status;
    private long rateDownload;
    private long rateUpload;
    private long uploadedEver;
    private long downloadedEver;
    private String downloadDir;
    private long eta;
}