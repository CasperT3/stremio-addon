package com.stremio.addon.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("torrent_info")
public class TorrentInfoModel {
    @Id
    private Long id;
    @Column("search_id")
    private Integer searchId; // Representa la clave foránea a la tabla "search".
    private String name;
    private String status;
    private Integer downloadId;
    private byte[] content;
}
