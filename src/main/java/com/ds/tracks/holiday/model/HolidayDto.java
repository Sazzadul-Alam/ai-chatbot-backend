package com.ds.tracks.holiday.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HolidayDto {
    private String source;
    private String spaceId;
    private String workspaceId;
    private String subspaceId;
    private List<Holiday> holidays;
}
