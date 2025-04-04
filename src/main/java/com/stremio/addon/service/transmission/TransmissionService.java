package com.stremio.addon.service.transmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stremio.addon.configuration.TransmissionConfiguration;
import com.stremio.addon.mapper.TorrentMapper;
import com.stremio.addon.model.TorrentInfoModel;
import com.stremio.addon.repository.SearchRepository;
import com.stremio.addon.repository.TorrentInfoRepository;
import com.stremio.addon.service.tmdb.TmdbService;
import com.stremio.addon.service.transmission.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class TransmissionService {

    private final TmdbService tmdbService;
    private final TransmissionConfiguration configuration;
    private final RestTemplate restTemplate;
    private final TorrentInfoRepository torrentInfoRepository;
    private final SearchRepository searchRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String sessionId;

    public TransmissionService(TmdbService tmdbService, TransmissionConfiguration configuration, @Qualifier("restTemplateJson") RestTemplate restTemplate, TorrentInfoRepository torrentInfoRepository, SearchRepository searchRepository) {
        this.tmdbService = tmdbService;
        this.configuration = configuration;
        this.restTemplate = restTemplate;
        this.torrentInfoRepository = torrentInfoRepository;
        this.searchRepository = searchRepository;
    }

    // 🔹 Método centralizado para manejar excepciones
    private RuntimeException handleException(String message) {
        log.error("{}", message);
        return new RuntimeException(message);
    }

    // 🔹 Genera los headers para la autenticación
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(configuration.getUsername(), configuration.getPassword());
        if (sessionId != null) headers.set("X-Transmission-Session-Id", sessionId);
        return headers;
    }

    // 🔹 Genera un magnet link desde un archivo
    public String generateMagnetLink(String torrentFilePath) throws IOException {
        log.info("Generating magnet link for file: {}", torrentFilePath);
        byte[] torrentData = readFileBytes(torrentFilePath);
        return generateMagnetLink(torrentData);
    }

    // 🔹 Genera un magnet link desde un byte[]
    public String generateMagnetLink(byte[] torrentData) {
        String infoHash = computeInfoHash(torrentData);
        String magnetLink = "magnet:?xt=urn:btih:" + infoHash;
        log.info("Magnet link generated: {}", magnetLink);
        return magnetLink;
    }

    // 🔹 Computa el hash SHA-1 del torrent
    private String computeInfoHash(byte[] torrentData) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(torrentData);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) {
            throw handleException("Error computing info hash");
        }
    }

    // 🔹 Añadir torrent desde archivo
    public TorrentInfo addTorrentFile(String torrentFilePath) throws IOException {
        log.info("Adding torrent file: {}", torrentFilePath);
        String base64 = encodeFileToBase64(torrentFilePath);
        return uploadTorrentAndGetId(base64);
    }

    // 🔹 Añadir torrent desde la base de datos
    public TorrentInfo addTorrent(Integer id) {
        log.info("Adding torrent from ID: {}", id);
        return torrentInfoRepository.findById(id)
                .map(torrentInfoModel -> {
                    String torrentBase64 = Base64.getEncoder().encodeToString(torrentInfoModel.getContent());
                    TorrentInfo torrentInfo = uploadTorrentAndGetId(torrentBase64);
                    updateTorrentInfoModel(torrentInfoModel, torrentInfo.getId(), TorrentStatus.DOWNLOADING);
                    return torrentInfo;
                })
                .orElseThrow(() -> handleException("Torrent with ID " + id + " not found in repository."));
    }

    // 🔹 Configurar webhook en Transmission
    public void configureWebhook(String webhookScript) {
        log.info("Configuring webhook with script: {}", webhookScript);
        TransmissionRequest request = TransmissionRequest.builder()
                .method("session-set")
                .arguments(SessionSetArguments.builder().scriptTorrentDoneFilename(webhookScript).build())
                .build();

        try {
            handleRequestWithSession(request);
            log.info("Webhook configured successfully.");
        } catch (Exception e) {
            throw handleException("Failed to configure webhook");
        }
    }

    public TorrentInfo checkTorrentStatus(Integer id, Integer ... args) {
        return searchRepository.findByTmdbId(id)
                .stream().findFirst().flatMap(searchModel -> searchModel.getTorrents()
                        .stream().filter(torrentInfoModel -> !torrentInfoModel.getStatus().equals("PENDING"))
                        .findFirst()).map(TorrentMapper.INSTANCE::mapToTorrentInfo)
                .orElseThrow(() -> handleException("Torrent with ID " + id + " not found in repository."));

    }

    // 🔹 Consultar el estado de un torrent
    public TorrentInfo checkTorrentProgress(int torrentId, Integer ... args) {
        log.info("Checking progress for torrent ID: {}", torrentId);

        return torrentInfoRepository.findById(torrentId)
                .map(torrentInfoModel -> {
                    TorrentStatus status = TorrentStatus.valueOf(torrentInfoModel.getStatus());
                    if (status == TorrentStatus.DOWNLOADING) {
                        return checkTorrentProgressInServer(torrentInfoModel.getDownloadId());
                    }
                    return null;
                })
                .orElseThrow(() -> handleException("Torrent with ID " + torrentId + " not found in repository."));
    }

    private TorrentInfo checkTorrentProgressInServer(int torrentId) {
        TransmissionRequest request = TransmissionRequest.builder()
                .method("torrent-get")
                .arguments(TorrentGetArguments.builder()
                        .ids(List.of(torrentId))
                        .fields(List.of("id", "name", "percentDone", "status", "totalSize", "downloadDir"))
                        .build())
                .build();

        try {
            TransmissionResponse response = handleRequestWithSession(request);
            TorrentGetResponse torrentGetResponse = objectMapper.convertValue(response.getArguments(), TorrentGetResponse.class);

            return Optional.ofNullable(torrentGetResponse.getTorrents())
                    .flatMap(torrents -> torrents.stream().findFirst())
                    .map(torrentInfo -> {
                        log.info("Torrent status retrieved: {}", torrentInfo);
                        torrentInfoRepository.findByDownloadId(torrentId)
                                .ifPresent(model -> updateTorrentInfoModel(model, torrentId, torrentInfo.getStatus()));
                        return torrentInfo;
                    })
                    .orElseThrow(() -> handleException("Torrent with ID " + torrentId + " not found."));
        } catch (Exception e) {
            throw handleException("Error checking torrent status");
        }

    }

    // 🔹 Subir un torrent y obtener su ID
    private TorrentInfo uploadTorrentAndGetId(String torrentBase64) {
        log.info("Uploading torrent...");
        TransmissionRequest request = TransmissionRequest.builder()
                .method("torrent-add")
                .arguments(TorrentAddArguments.builder().metainfo(torrentBase64).build())
                .build();

        TransmissionResponse response = handleRequestWithSession(request);
        if (response != null && response.getArguments() != null) {
            TorrentAddResponse torrentAddResponse = objectMapper.convertValue(response.getArguments(), TorrentAddResponse.class);
            return Optional.ofNullable(torrentAddResponse.getTorrentAdded()).orElse(torrentAddResponse.getTorrentDuplicate());
        }
        throw handleException("Torrent uploaded, but no ID returned by Transmission.");
    }

    // 🔹 Manejo centralizado de requests a Transmission
    private TransmissionResponse handleRequestWithSession(TransmissionRequest payload) {
        try {
            ResponseEntity<TransmissionResponse> response = restTemplate.exchange(
                    configuration.getTransmissionUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(payload, createAuthHeaders()),
                    TransmissionResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.Conflict e) {
            sessionId = Objects.requireNonNull(e.getResponseHeaders()).getFirst("X-Transmission-Session-Id");
            if (sessionId != null) {
                log.info("Updated session ID: {}", sessionId);
                return handleRequestWithSession(payload);
            }
            throw handleException("Session ID could not be updated");
        }
    }

    // 🔹 Verificar descargas activas
    public List<TorrentInfo> checkActiveDownloads() {
        log.info("Checking active downloads...");
        TransmissionRequest request = TransmissionRequest.builder()
                .method("torrent-get")
                .arguments(TorrentGetArguments.builder().fields(List.of("id", "name", "percentDone", "status")).build())
                .build();

        TransmissionResponse response = handleRequestWithSession(request);
        var torrents = objectMapper.convertValue(response.getArguments(), TorrentGetResponse.class).getTorrents();
        torrents.forEach(torrentInfo -> torrentInfoRepository.findByDownloadId(torrentInfo.getId())
                .ifPresent(model -> updateTorrentInfoModel(model, torrentInfo.getId(), torrentInfo.getStatus())));
        return torrents;
    }

    // 🔹 Eliminar torrent
    public void removeTorrent(int torrentId, boolean deleteData) {
        log.info("Removing torrent ID: {}", torrentId);
        TransmissionRequest request = TransmissionRequest.builder()
                .method("torrent-remove")
                .arguments(TorrentRemoveArguments.builder().ids(List.of(torrentId)).deleteLocalData(deleteData).build())
                .build();

        handleRequestWithSession(request);
        log.info("Torrent ID {} removed successfully.", torrentId);
    }

    // 🔹 Leer archivo como byte[]
    private byte[] readFileBytes(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw handleException("File not found: " + filePath);
        return Files.readAllBytes(file.toPath());
    }

    // 🔹 Codificar archivo a Base64
    private String encodeFileToBase64(String filePath) throws IOException {
        return Base64.getEncoder().encodeToString(readFileBytes(filePath));
    }

    // 🔹 Actualizar estado del torrent en BD
    private void updateTorrentInfoModel(TorrentInfoModel model, int downloadId, TorrentStatus status) {
        model.setDownloadId(downloadId);
        model.setStatus(status.toString());
        torrentInfoRepository.save(model);
    }
}
