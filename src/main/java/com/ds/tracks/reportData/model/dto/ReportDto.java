package com.ds.tracks.reportData.model.dto;

import com.ds.tracks.tasks.model.dto.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ReportDto {
    private String workspaceId;
    private String workspaceName;
    private String spaceId;
    private List<String> project;
    private List<String> segment;
    private List<String> type;
    private String invoiceType;
    private List<String> category;
    private List<String> status;
    private List<String> priority;
    private List<String> tags;
    private String spaceName;
    private String subSpaceId;
    private String subSpaceName;
    private String taskId;
    private String taskName;
    private String subTaskId;
    private String subTaskName;
//    private String type;
//    private String category;
//    private String priority;
//    private String status;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortOrder;
    private Date startDate;
    private Date endDate;
    private String startDateString;
    private String endDateString;
    private String fileType;
    private String reportType;
}
