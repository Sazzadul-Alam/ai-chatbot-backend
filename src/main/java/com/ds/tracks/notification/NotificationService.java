package com.ds.tracks.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface NotificationService {
    ResponseEntity<?> getAll(String workspaceId);

    ResponseEntity<String> subscribe(String requestId, String workspaceId);

    ResponseEntity<?> read(String id);
    void sendNotifications(List<String> userId, String source, String sourceId, String workspaceId, String spaceId, String message);

    ResponseEntity<String> unsubscribe(String id);
}
