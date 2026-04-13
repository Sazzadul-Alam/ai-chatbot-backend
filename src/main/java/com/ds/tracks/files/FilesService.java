package com.ds.tracks.files;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface FilesService {
    ResponseEntity<?> upload(String workspaceId, String spaceId, String subspaceId, MultipartFile file, String source, String sourceId);

    ResponseEntity<?> download(String id, HttpServletResponse response);

    ResponseEntity<?> getAll(String sourceId, String source);

    ResponseEntity<?> delete(String id);

    ResponseEntity<?> uploadSpaceDetails(String id, String type, MultipartFile file);

    ResponseEntity<?> deleteSpaceDetails(String id);

    void downloadSpaceDetails(String id, HttpServletResponse response);
}
