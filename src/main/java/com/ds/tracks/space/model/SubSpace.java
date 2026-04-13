package com.ds.tracks.space.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@Document(collection = "sub_spaces")
@NoArgsConstructor
@AllArgsConstructor
public class SubSpace {
    @Id
    private String id;
    private String spaceId;
    private List<String> menus;
    private SpaceConfigurations configurations;
    private String workspaceId;
    private Date plannedStartDate;
    private Date plannedEndDate;
    private String folder;
    private String name;
    private String folderId;
    private String color;
    private String createdBy;
    private Date createdAt;
    private String updatedBy;
    private Date updatedAt;


}
