package com.ds.tracks.issue;

import com.ds.tracks.tasks.model.dto.TasksHistory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document("issues")
public class Issue {
    @Id
    private String id;
    private String workspaceId;
    private String spaceId;
    private String subspaceId;
    private String name;
    private String tag;
    private List<String> tags;
    private String status;
    private String severity;
    private String preConditions;
    private String testData;
    private String testSteps;
    private String expectedResult;
    private String actualResult;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
    private String trackedAt;
    private String trackedBy;
    private List<String> tasks;
    private List<String> testCases;
    private List<String> comments;
    private List<TasksHistory> history;
}
