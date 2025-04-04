package com.stremio.addon.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
public class TorrentDownloaderService {

    public byte[] downloadTorrent(String torrentUrl) {
        log.info("Starting download of torrent file from URL: {}", torrentUrl);
        try {
            HttpURLConnection connection = establishConnection(new URL(torrentUrl));
            return downloadData(connection);
        } catch (Exception e) {
            log.error("Error downloading torrent file from URL: {}", torrentUrl, e);
            return null;
        }
    }

    private HttpURLConnection establishConnection(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        int statusCode = connection.getResponseCode();

        if (isRedirect(statusCode)) {
            String redirectUrl = connection.getHeaderField("Location");
            log.info("Redirect detected. Original URL: {}, Redirecting to: {}", url, redirectUrl);
            return establishConnection(new URL(redirectUrl));
        }

        if (statusCode != HttpURLConnection.HTTP_OK) {
            log.error("Failed to establish connection. HTTP status code: {}", statusCode);
            throw new RuntimeException("Failed to download torrent file, HTTP response code: " + statusCode);
        }

        log.info("Connection established with HTTP status code: {}", statusCode);
        return connection;
    }

    private byte[] downloadData(HttpURLConnection connection) throws Exception {
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            log.info("Downloading data...");
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            log.info("Download completed successfully.");
            return byteArrayOutputStream.toByteArray();
        }
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_MOVED_TEMP;
    }
}