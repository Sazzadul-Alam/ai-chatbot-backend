package com.ds.tracks.space.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document("folder")
public class Folder {
    @Id
    private String id;
    private String workspaceId;
    private String spaceId;
    private String name;
    private String category;
    private Date plannedStartDate;
    private Date plannedEndDate;
    private List<String> subspaces;

    private Date actualStartDate;
    private Date actualEndDate;
}
