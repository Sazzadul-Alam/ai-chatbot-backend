package com.ds.tracks.tasks.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "task_dependency")
public class TaskDependency {
    @Id
    private String id;
    private String taskId;
    private String dependency;

}
