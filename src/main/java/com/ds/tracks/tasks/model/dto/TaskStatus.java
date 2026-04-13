package com.ds.tracks.tasks.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatus {
    private int position;
    private String name;

    public TaskStatus(int position, String name) {
        this.position = position;
        this.name = name;
    }

    public TaskStatus() {
    }
}
