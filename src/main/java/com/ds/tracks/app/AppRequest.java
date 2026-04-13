package com.ds.tracks.app;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AppRequest {
    private String workspace;
    private String project;
    private String segment;
    private String folder;
    private Date startDate;
    private String startDateString;
    private Date endDate;
    private String endDateString;
    private Boolean allTask;
}
