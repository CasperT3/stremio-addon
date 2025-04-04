package com.stremio.addon.service;

import com.stremio.addon.model.SearchModel;
import com.stremio.addon.repository.SearchRepository;
import com.stremio.addon.repository.TorrentInfoRepository;
import com.stremio.addon.service.transmission.TransmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessor {

    private final TransmissionService transmissionService;
    private final FileRenameService fileRenameService;
    private final TorrentInfoRepository torrentInfoRepository;
    private final SearchRepository searchRepository;

    public void processDownloadComplete(Map<String, Object> payload) {
        try {
            String torrentName = (String) payload.get("name");
            String downloadDir = (String) payload.get("downloadDir");
            Path originalFilePath = Paths.get(downloadDir, torrentName);

            log.info("Processing download complete for torrent: {}, Directory: {}", torrentName, downloadDir);

            transmissionService.checkActiveDownloads();
            handleFileProcessing(torrentName, originalFilePath);

        } catch (Exception e) {
            log.error("Error processing download complete notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process download complete notification", e);
        }
    }

    private void handleFileProcessing(String torrentName, Path originalFilePath) {
        torrentInfoRepository.findByName(torrentName).ifPresent(torrentInfo -> {
            searchRepository.findById(torrentInfo.getSearchId()).ifPresent(search -> {
                try {
                    renameFileForPlex(originalFilePath, search);
                    torrentInfoRepository.save(torrentInfo);
                } catch (Exception e) {
                    log.error("Error renaming file for Plex: {}", e.getMessage(), e);
                    throw new RuntimeException("File renaming failed", e);
                }
            });
        });
    }

    private void renameFileForPlex(Path originalFilePath, SearchModel search) throws Exception {
        String type = search.getType();
        String title = search.getTitle();

        if ("movie".equalsIgnoreCase(type)) {
            fileRenameService.renameFileForPlex(originalFilePath.toString(), type, title, search.getYear(), null);
        } else if ("series".equalsIgnoreCase(type)) {
            fileRenameService.renameFileForPlex(originalFilePath.toString(), type, title, search.getSeason(), search.getEpisode());
        } else {
            log.warn("Unknown search type: {}", type);
        }
    }
}
