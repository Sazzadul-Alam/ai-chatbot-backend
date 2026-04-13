package com.ds.tracks.space.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class FolderDto {
    private String id;
    private String workspaceId;
    private String spaceId;
    private String name;
    private String category;
    private Date startDate;
    private Date endDate;
    private Boolean app;
    private List<String> subspaces;
}
