package com.stremio.addon.service;

import com.stremio.addon.configuration.AddonConfiguration;
import com.stremio.addon.model.SearchModel;
import com.stremio.addon.repository.SearchRepository;
import com.stremio.addon.repository.TorrentInfoRepository;
import com.stremio.addon.service.transmission.TransmissionService;
import com.stremio.addon.service.transmission.dto.TorrentInfo;
import com.stremio.addon.service.transmission.dto.TorrentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class TorrentMonitorService {

    private final TransmissionService transmissionService;
    private final FileRenameService fileRenameService;
    private final TorrentInfoRepository torrentInfoRepository;
    private final SearchRepository searchRepository;
    private final AddonConfiguration addonConfiguration;

    public TorrentMonitorService(TransmissionService transmissionService,
                                 FileRenameService fileRenameService,
                                 TorrentInfoRepository torrentInfoRepository,
                                 SearchRepository searchRepository, AddonConfiguration addonConfiguration) {
        this.transmissionService = transmissionService;
        this.fileRenameService = fileRenameService;
        this.torrentInfoRepository = torrentInfoRepository;
        this.searchRepository = searchRepository;
        this.addonConfiguration = addonConfiguration;
    }

    //@Scheduled(fixedRateString = "${torrent.monitor.interval:60000}")
    public void monitorTorrents() {
        log.info("Checking active downloads...");
        List<TorrentInfo> activeTorrents = transmissionService.checkActiveDownloads();

        for (TorrentInfo torrent : activeTorrents) {
            String name = (String) torrent.getName();
            double percentDone = (double) torrent.getPercentDone();
            var status = torrent.getStatus();
            int torrentId = (int) torrent.getId(); // Obtener el ID del torrent

            log.info("Torrent: {}, Progress: {}%, Status: {}", name, percentDone * 100, status);

            if (status.equals(TorrentStatus.SEEDING) && percentDone == 1.0) { // 6 = Seeding, 1.0 = 100% completed
                handleCompletedTorrent(name, torrentId);
            }
        }
    }


    private void handleCompletedTorrent(String torrentName) {
        try {
            // Suponemos que el archivo descargado está en una ruta específica
            Path originalFilePath = Paths.get(addonConfiguration.getDownloadPath(), torrentName);
            handleFileProcessing(torrentName, originalFilePath);
        } catch (Exception e) {
            log.error("Error handling completed torrent {}: {}", torrentName, e.getMessage(), e);
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
        // Extract details from the search object
        String type = search.getType(); // Assumes Search has a type field ("movie" or "series")
        String title = search.getTitle();
        int yearOrSeason = type.equals("movie") ? search.getYear() : search.getSeason();
        Integer episode = search.getEpisode();

        String newPath = fileRenameService.renameFileForPlex(originalFilePath.toString(), type, title, yearOrSeason, episode);
        log.info("File renamed and moved to: {}", newPath);
    }

    private void handleCompletedTorrent(String torrentName, int torrentId) {
        try {
            // Suponemos que el archivo descargado está en una ruta específica
            Path originalFilePath = Paths.get("/ruta/descargas", torrentName);
            handleFileProcessing(torrentName, originalFilePath);

            // Eliminar el torrent de Transmission
            transmissionService.removeTorrent(torrentId, false); // Cambia a true si quieres eliminar los datos locales
            log.info("Torrent {} removed from Transmission.", torrentName);
        } catch (Exception e) {
            log.error("Error handling completed torrent {}: {}", torrentName, e.getMessage(), e);
        }
    }

}
