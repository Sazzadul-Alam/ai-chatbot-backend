package com.ds.tracks.space.model.dto;


import com.ds.tracks.tasks.model.dto.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SubSpaceDto {
    private String name;
    private String color;
    private String finalStage;
    private Date startDate;
    private Date endDate;
    private String createdBy;
    private String createdAt;
    private String spaceId;
    private String workspaceId;
    private List<String> types;
    private List<String> menus;
    private List<TaskStatus> status;
    private List<String> category;
}
