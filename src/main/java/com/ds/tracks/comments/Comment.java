package com.ds.tracks.comments;

import com.ds.tracks.commons.utils.CollectionName;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.Date;

@Getter
@Setter
@Document(CollectionName.comments)
public class Comment {
    @Id
    private String id;
    private String workspaceId;
    private String spaceId;
    private String subspaceId;
    private String sourceId;
    private String source;
    private String userId;
    private String comment;
    private Date date;
}
