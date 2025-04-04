package com.stremio.addon.controller;

import com.stremio.addon.service.transmission.TransmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/transmission", produces = "application/json")
public class TransmissionController {

    private final TransmissionService transmissionService;

    public TransmissionController(TransmissionService transmissionService) {
        this.transmissionService = transmissionService;
    }

    @PostMapping("/add-torrent")
    public ResponseEntity<String> addTorrentFile(@RequestParam String torrentFilePath) {
        try {
            transmissionService.addTorrentFile(torrentFilePath);
            return ResponseEntity.ok("Torrent successfully added.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/generate-magnet")
    public ResponseEntity<String> generateMagnet(@RequestParam String torrentFilePath) {
        try {
            String magnetLink = transmissionService.generateMagnetLink(torrentFilePath);
            return ResponseEntity.ok(magnetLink);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/update-script")
    public ResponseEntity<String> updateScript(@RequestParam String scriptPath) {
        try {
            transmissionService.configureWebhook(scriptPath);
            return ResponseEntity.ok("Script path updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/active-downloads")
    public ResponseEntity<?> getActiveDownloads() {
        try {
            return ResponseEntity.ok(transmissionService.checkActiveDownloads());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


}
