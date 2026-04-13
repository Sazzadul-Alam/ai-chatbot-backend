package com.ds.tracks.files;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoRepository extends MongoRepository<FileInfo, String> {

    FileInfo findFirstById(String id);

    List<FileInfo> findBySourceIdAndSource(String sourceId, String source);
}
