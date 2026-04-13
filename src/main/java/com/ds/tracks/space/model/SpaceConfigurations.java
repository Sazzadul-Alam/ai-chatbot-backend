package com.ds.tracks.space.model;

import com.ds.tracks.holiday.model.Holiday;
import com.ds.tracks.tasks.model.dto.TaskStatus;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SpaceConfigurations {
    private List<String> category;
    private List<TaskStatus> status;
    private List<String> type;
    private List<String> historicType;
    private List<String> historicCategory;
    private List<String> weekend;
    private String lockStatus;
    private List<Holiday> holiday;
    private Double workHour;
}
