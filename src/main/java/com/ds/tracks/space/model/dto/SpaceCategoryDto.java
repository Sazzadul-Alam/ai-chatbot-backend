package com.ds.tracks.space.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SpaceCategoryDto {
    private String spaceId;
    private String category;
    private List<String> templates;
    private List<String> selectedTasks;
    private Map<String, String> assignedUsers;
}
