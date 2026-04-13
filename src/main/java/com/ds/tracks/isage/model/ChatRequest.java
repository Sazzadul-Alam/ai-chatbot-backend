package com.ds.tracks.isage.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Data
@Document(collection = "chat_request")
public class ChatRequest {
    @Id
    private String id;
    private Map<String, Object> request;
    private Date createdAt = new Date();
}
