package com.ds.tracks.template;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document("templates")
public class CustomTemplate {
    @Id
    private String id;
    private String name;
    private String category;
    private String workspaceId;
    private String createdBy;
    private Date createdAt;
}
