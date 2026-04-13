package com.ds.tracks.tasks.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TasksHistory {
    private String action;
    private String userId;
    private String fullName;
    private Date date;

    public  TasksHistory(String action, String userId, String fullName) {
        this.action = action;
        this.userId = userId;
        this.fullName = fullName;
        this.date = new Date();
    }
}
