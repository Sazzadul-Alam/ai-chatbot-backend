package com.ds.tracks.files;

import com.ds.tracks.commons.utils.CollectionName;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(CollectionName.file_info)
public class FileInfo {
    @Id
    private String id;
    private String workspaceId;
    private String spaceId;
    private String folderId;
    private String subspaceId;

    private String source;
    private String sourceId;

    private String filename;
    private String category;
    private String extension;
    private Date uploadedAt;
    private String uploadedBy;

    private String savedFilename;
    private String savedLocation;

}
