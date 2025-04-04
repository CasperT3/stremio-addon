package com.stremio.addon.repository;

import com.stremio.addon.model.TorrentInfoModel;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TorrentInfoRepository extends CrudRepository<TorrentInfoModel, Integer>{
    Optional<TorrentInfoModel> findByName(String torrentName);

    Optional<TorrentInfoModel> findByDownloadId(int torrentId);
}
