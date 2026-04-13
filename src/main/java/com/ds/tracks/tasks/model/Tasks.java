package com.ds.tracks.tasks.model;

import com.ds.tracks.backlog.BacklogDto;
import com.ds.tracks.tasks.model.dto.TasksHistory;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "tasks")
public class Tasks {
    @Id
    private String id;
    private String generatedId;
    private String spaceId;
    private String subSpaceId;
    private String name;
    private String type;
    private String category;
    private String description;
    private Integer priority;
    private Integer severity;
    private Double duration;
    private Double actualDuration;
    private Double storyPoint;
    private Integer position;

    private Date startDate; // Actual Start Date
    private Date deadline; // Expected End Date
    private Date closingDate; // Actual Closing Date

    private String issuedTo;
    private String issuedBy;
    private Date issueDate; // Creation date

    private List<TasksHistory> history;
    private List<String> backlogs;
    private List<String> tags;
    private List<String> issues;
    private List<String> testCases;
    private List<String> tasks;
    private List<String> assignedUsers;

    private Double completion;

    private String status;
    private Boolean overtime;
    private Boolean locked;

}
