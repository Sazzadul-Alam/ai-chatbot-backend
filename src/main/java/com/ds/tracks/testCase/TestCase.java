package com.ds.tracks.testCase;

import com.ds.tracks.tasks.model.dto.TasksHistory;
import com.ds.tracks.tasks.model.dto.TasksVersion;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@Document("test_case")
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    @Id
    private String id;
    private String generatedId;
    private String workspaceId;
    private String spaceId;
    private String subspaceId;
    private String priority;
    private String previousSubspaceId;
    private String folderId;
    private String name;
    private String manualId;
    private String testData;
    private String testSteps;
    private String preCondition;
    private String expectedResult;
    private String actualResult;
    private String status;


    private List<String> tags;
    private List<String> tasks;
    private List<String> issues;
    private List<String> backlogs;
    private List<String> comments;
    private List<TasksHistory> history;
    private List<TasksVersion> versions;

    private Date createdAt;
    private String createdBy;
}
