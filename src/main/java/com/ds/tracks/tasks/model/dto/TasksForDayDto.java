package com.ds.tracks.tasks.model.dto;

import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.tasks.model.Tasks;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TasksForDayDto {
    private List<IdNameRelationDto> counts;
    private List<Tasks> tasks;

    public TasksForDayDto() {
        this.counts = new ArrayList<>();
        this.tasks = new ArrayList<>();
    }
}
