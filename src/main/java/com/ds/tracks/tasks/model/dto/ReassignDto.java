package com.ds.tracks.tasks.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReassignDto {
    private String id;
    private List<ModificationDto> changes;
    private Boolean reassignFomToday;
}
