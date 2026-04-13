package com.ds.tracks.notification;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

@Slf4j
@Getter
@Setter
@Document("notifications")
public class NotificationData implements Serializable {

    @Id
    private String id;
    private String userId;
    private String source;
    private String sourceId;
    private String workspaceId;
    private String spaceId;
    private String message;
    private Date date;
    private boolean read;

    public NotificationData(String userId, String source, String sourceId, String workspaceId, String spaceId, String message) {
        this.userId = userId;
        this.source = source;
        this.sourceId = sourceId;
        this.workspaceId = workspaceId;
        this.spaceId = spaceId;
        this.message = message;
        this.date = new Date();
        this.read = false;
    }

    public NotificationData() {
    }

}
