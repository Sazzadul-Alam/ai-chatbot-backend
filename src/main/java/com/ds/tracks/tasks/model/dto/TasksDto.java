package com.ds.tracks.tasks.model.dto;

import com.ds.tracks.backlog.BacklogDto;
import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.tasks.model.SubTasks;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class TasksDto {
    private String id;
    private String name;
    private String spaceId;
    private String subSpaceId;
    private String workspaceId;
    private Double storyPoint;
    private String type;
    private String category;
    private String description;
    private String status;
    private Integer priority;
    private Integer severity;
    private Date deadline;
    private List<String> assignedUsers;
    private String deadlineString;
    private Double duration;
    private String issuedTo;
    private String taskDate;
    private Date startDate;
    private String startDateString;
    private String draftId;
    private Date createdAt;
    private String parentTaskId;
    private List<SubTasks> subtasks;
    private List<TasksHistory> history;
    private List<EffortLog> efforts;
//    private List<BacklogDto> backlogs;
    private List<String> tags;
    private List<String> backlogs;
    private List<String> tasks;
    private List<String> testCases;
    private Boolean overtime;
    private Boolean app;
    private String operationType;
    private List<String> issues;
}
