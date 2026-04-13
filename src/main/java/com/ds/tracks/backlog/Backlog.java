package com.ds.tracks.backlog;

import com.ds.tracks.tasks.model.dto.TasksHistory;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("backlog")
public class Backlog {
    @Id
    private String id;
    private String spaceId;
    private String subSpaceId;
    private String generatedId;
    private Double storyPoint;
    private String name;
    private String status;
    private List<String> tasks;
    private List<String> tags;
    private List<String> testCases;
    private String description;
    private Date createdAt;
    private String createdBy;
    private List<TasksHistory> history;

    private Date updatedAt;
    private String updatedBy;

    private String parentId;
    private String parentGeneratedId;

}
