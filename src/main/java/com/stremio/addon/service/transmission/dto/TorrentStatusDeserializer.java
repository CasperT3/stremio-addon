package com.stremio.addon.service.transmission.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

class TorrentStatusDeserializer extends JsonDeserializer<TorrentStatus> {
    @Override
    public TorrentStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        int statusCode = p.getIntValue();
        return TorrentStatus.fromCode(statusCode);
    }
}
