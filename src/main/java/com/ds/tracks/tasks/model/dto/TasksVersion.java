package com.ds.tracks.tasks.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TasksVersion {
    private String spaceId;
    private String subspaceId;
    private Date createdAt;
    private String createdBy;
    private Integer version;
    private String status;

    public TasksVersion() {
    }

    public TasksVersion(String spaceId, String subspaceId, String createdBy, String status, Integer version) {
        this.subspaceId = subspaceId;
        this.spaceId = spaceId;
        this.createdBy = createdBy;
        this.status = status;
        this.createdAt = new Date();
        this.version = version;
    }
}
