package com.stremio.addon.mapper;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import com.stremio.addon.controller.dto.Stream;
import com.stremio.addon.controller.dto.TorrentDto;
import com.stremio.addon.model.TorrentInfoModel;
import com.stremio.addon.service.transmission.dto.TorrentInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface TorrentMapper {
    TorrentMapper INSTANCE = Mappers.getMapper(TorrentMapper.class);

    TorrentInfo mapToTorrentInfo(TorrentInfoModel model);

    @Mapping(source = "content", target = "infoHash", qualifiedByName = "getInfoHash")
    @Mapping(source = "content", target = "behaviorHints", qualifiedByName = "getBehaviorHints")
    @Mapping(source = "content", target = "sources", qualifiedByName = "getTrackers")
    @Mapping(source = "content", target = "description", qualifiedByName = "getFilename")
    @Mapping(target = "name", constant = "Spanish Torrent")
    Stream map(TorrentInfoModel model);

    @Mapping(source = "data", target = "content")
    @Mapping(source = "data", target = "name", qualifiedByName = "getFilename")
    TorrentInfoModel map(byte[] data);

    @Named("getInfoHash")
    default String getInfoHash(byte[] data) {
        try {
            Map<String, BEncodedValue> infoMap = decodeBytesToMap(data);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] infoBytes = BEncoder.encode(infoMap).array();
            return bytesToHex(digest.digest(infoBytes));
        } catch (Exception e) {
            throw new RuntimeException("Error obtaining infoHash from info map", e);
        }
    }

    @Named("getBehaviorHints")
    default Map<String, Object> getBehaviorHints(byte[] data) throws Exception {
        if (data == null) {
            throw new RuntimeException("Data is null");
        }
        Map<String, BEncodedValue> infoData = decodeBytesToMap(data);
        Map<String, BEncodedValue> infoMap = infoData.get("info").getMap();
        Map<String, Object> behaviorHints = new HashMap<>();
        try {
            if (infoMap.containsKey("bingeGroup")) {
                behaviorHints.put("bingeGroup", infoMap.get("bingeGroup").getString());
            }
            if (infoMap.containsKey("filename")) {
                behaviorHints.put("filename", infoMap.get("filename").getString());
            }
            if (infoMap.containsKey("videoSize")) {
                behaviorHints.put("videoSize", infoMap.get("videoSize").getLong());
            }
            if (infoMap.containsKey("name")) {
                behaviorHints.put("filename", infoMap.get("name").getString());
            }
            behaviorHints.put("videoSize", extractTotalFileSize(infoMap));
            return behaviorHints;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting behaviorHints from torrent info map", e);
        }
    }

    private long extractTotalFileSize(Map<String, BEncodedValue> infoMap) {
        try {
            if (infoMap.containsKey("length")) {
                return infoMap.get("length").getLong();
            }
            if (infoMap.containsKey("files")) {
                long totalSize = 0;
                for (BEncodedValue fileEntry : infoMap.get("files").getList()) {
                    totalSize += fileEntry.getMap().get("length").getLong();
                }
                return totalSize;
            }
            throw new RuntimeException("No file size information found in torrent");
        } catch (Exception e) {
            throw new RuntimeException("Error extracting total file size from info map", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Named("getFilename")
    static String getFilename(byte[] data) throws Exception {
        if (data == null) {
            throw new RuntimeException("Data is null");
        }
        Map<String, BEncodedValue> infoData = decodeBytesToMap(data);
        Map<String, BEncodedValue> infoMap = infoData.get("info").getMap();
        if (infoMap.containsKey("name")) {
            try {
                return infoMap.get("name").getString();
            } catch (Exception e) {
                throw new RuntimeException("Error extracting filename from info map", e);
            }
        }
        throw new RuntimeException("Filename not found in torrent info map");
    }

    @Named("getTrackers")
    default List<String> getTrackers(byte[] data) {
        try {
            Map<String, BEncodedValue> infoMap = decodeBytesToMap(data);
            List<String> trackerList = new ArrayList<>();
            if (infoMap.containsKey("announce")) {
                trackerList.add("tracker:" + infoMap.get("announce").getString());
            }
            if (infoMap.containsKey("announce-list")) {
                for (BEncodedValue announceList : infoMap.get("announce-list").getList()) {
                    for (BEncodedValue trackerEntry : announceList.getList()) {
                        trackerList.add("tracker:" + trackerEntry.getString());
                    }
                }
            }
            return trackerList;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting trackers from torrent map", e);
        }
    }

    private static Map<String, BEncodedValue> decodeBytesToMap(byte[] data) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            return BDecoder.decode(inputStream).getMap();
        }
    }

    @Mapping(source = "torrentInfoModel", target = "size", qualifiedByName = "getTotalSize")
    TorrentDto mapToDto(TorrentInfoModel torrentInfoModel);

    @Named("getTotalSize")
    default Long getTotalSize(TorrentInfoModel torrentInfoModel) {
        try {
            var behaviorHints = getBehaviorHints(torrentInfoModel.getContent());
           	return (Long) behaviorHints.get("videoSize");
        } catch (Exception e) {
            throw new RuntimeException("Error extracting total file size from info map", e);
        }
    }

}
