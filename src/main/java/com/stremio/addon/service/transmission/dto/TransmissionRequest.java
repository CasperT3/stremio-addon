package com.stremio.addon.service.transmission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransmissionRequest {
    private List<Integer> ids;
    private String method;
    private RequestArguments arguments;
}
