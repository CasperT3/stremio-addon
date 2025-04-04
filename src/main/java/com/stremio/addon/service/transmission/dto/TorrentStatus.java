package com.stremio.addon.service.transmission.dto;

public enum TorrentStatus {
    STOPPED(0, "Stopped"),
    CHECK_WAIT(1, "Waiting to verify"),
    CHECKING(2, "Verifying"),
    DOWNLOAD_WAIT(3, "Waiting to download"),
    DOWNLOADING(4, "Downloading"),
    SEED_WAIT(5, "Waiting to seed"),
    SEEDING(6, "Seeding");

    private final int code;
    private final String description;

    TorrentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TorrentStatus fromCode(int code) {
        for (TorrentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TorrentStatus code: " + code);
    }
}
