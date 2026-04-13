package com.ds.tracks.reportData.model;

import com.ds.tracks.commons.models.UserPermissionGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "report_data")
@JsonIgnoreProperties
public class ReportData {
    @Id
    private String id;
    private String workspaceId;
    private String workspaceName;
    private String workspaceCreator;
    private String spaceId;
    private String spaceName;
    private String spaceCreator;
    private List<UserPermissionGroup> spaceParticipants;
    private String folderId;
    private String folderName;
    private String subSpaceId;
    private String subSpaceName;
    private String subSpaceCreator;
    private List<UserPermissionGroup> subSpaceParticipants;
    private String taskId;
    private String taskGeneratedId;
    private String taskName;
    private String taskIssuedBy;
    private String taskIssuedTo;
    private Date taskIssuedDate;
    private String taskType;
    private String taskCategory;
    private String taskDescription;
    private Integer taskPriority;
    private Integer taskSeverity;
    private Double taskDuration;
    private Date taskStartDate;
    private Date taskDeadline;
    private String taskStatus;
    private String subTaskId;
    private String subTaskGeneratedId;
    private String subTaskName;
    private String subTaskIssuedBy;
    private String subTaskIssuedTo;
    private Date subTaskIssuedDate;
    private String subTaskType;
    private String subTaskCategory;
    private String subTaskDescription;
    private Integer subTaskPriority;
    private Integer subTaskSeverity;
    private Double subTaskDuration;
    private Date subTaskStartDate;
    private Date subTaskDeadline;
    private String subTaskStatus;
    private String effortLogger;
    private Double effortDuration;
    private Date effortStartDate;
    private Date effortEndDate;
    private Double actualDuration;
    private Date actualStartDate;
    private Date actualEndDate;
    private Double completion;
    private String status;

}
