package com.ds.tracks.tasks.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TasksLink {
    String id;
    String name;
    String type; // Parent | Child
}
