package com.ds.tracks.template;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document("template_tasks")
public class CustomTemplateTasks {
    @Id
    private String id;
    private String templateId;
    private String name;
    private String description;
    private String category;
    private String type;
    private Integer priority;
    private Double storyPoint;
    private Double duration;
    private Integer position;
    private List<String> tags;
}
