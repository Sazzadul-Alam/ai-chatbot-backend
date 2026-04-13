package com.ds.tracks.effort.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "effort_log")
public class EffortLog {
    @Id
    private String id;
    private String workspaceId;
    private String spaceId;
    private String subspaceId;
    private String taskId;
    private String subTaskId;
    private Double duration;
    private Double completion;
    private String description;
    private String comment;
    private Date createdAt;
    private Date logDate;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
}
