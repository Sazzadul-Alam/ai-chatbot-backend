package com.ds.tracks.tasks.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("task_schedule")
public class TaskSchedule {
    @Id
    private String id;
    private String taskId;
    private String subTaskId;
    private String spaceId;
    private String subspaceId;
    private Date scheduleDate;
    private String dateString;
    private String assignedTo;
    private Double duration;

}
