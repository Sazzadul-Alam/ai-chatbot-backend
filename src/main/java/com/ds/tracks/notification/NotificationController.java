package com.ds.tracks.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/get")
    public ResponseEntity<?> getAll(@RequestParam String workspaceId){
        return notificationService.getAll(workspaceId);
    }
    @PostMapping("/subscribe")
    public ResponseEntity<String > subscribe(
            @RequestParam String id,
            @RequestParam String workspaceId){
        return notificationService.subscribe(id, workspaceId);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String id){
        return notificationService.unsubscribe(id);
    }
    @PostMapping("/read")
    public ResponseEntity<?> read(@RequestParam String id){
        return notificationService.read(id);
    }
}
