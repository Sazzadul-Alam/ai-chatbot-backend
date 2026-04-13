package com.ds.tracks.notification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Subscriptions {
    private String workspaceId;
    private String requestId;

    public Subscriptions() {
    }

    public Subscriptions(String workspaceId, String requestId) {
        this.workspaceId = workspaceId;
        this.requestId = requestId;
    }
}
