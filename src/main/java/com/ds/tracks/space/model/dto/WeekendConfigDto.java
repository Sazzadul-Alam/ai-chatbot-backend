package com.ds.tracks.space.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class WeekendConfigDto {
    private Map<String, Boolean> weekdays;
    private String spaceId;
    private String workspaceId;
    private String source;
    private String subspaceId;
}
