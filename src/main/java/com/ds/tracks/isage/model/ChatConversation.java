package com.ds.tracks.isage.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "chat_conversation")
public class ChatConversation {
    @Id
    private String id;
    private String loginId;
    private Object data;
    private String userId;
    private Date createdAt = new Date();
    private Date updatedAt;
}