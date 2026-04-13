package com.ds.tracks.notification;

import java.util.Map;

public interface NotificationDao {

    Map<String, Object> getNotifications(String userId, String workspaceId);
    void markRead(String id);
}
