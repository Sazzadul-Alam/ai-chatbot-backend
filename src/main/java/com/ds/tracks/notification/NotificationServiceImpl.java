package com.ds.tracks.notification;

import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationsRepository notificationsRepository;

    private final UserService userService;
    private final HashMap<String, List<Subscriptions>> users = new HashMap<>();

    @Override
    public ResponseEntity<?> getAll(String workspaceId) {
        return new ResponseEntity<>(notificationsRepository.getNotifications(userService.getCurrentUserId(), workspaceId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> subscribe(String requestId, String workspaceId) {
        if(isValidString(requestId) && isValidString(workspaceId) && Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())){
            String user = userService.getCurrentUserId();
            if(!users.containsKey(user)){
                users.put(user, new ArrayList<>());
            }
            users.get(user).add(new Subscriptions(workspaceId, requestId));
            return new ResponseEntity<>("Subscribed", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }


    @Override
    public ResponseEntity<?> read(String id) {
        notificationsRepository.markRead(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public void send(String userId, String workspaceId, Object message) {
        try{
            if(users.containsKey(userId)){
                for(Subscriptions subscription : users.get(userId)){
                    if(Objects.equals(subscription.getWorkspaceId(), workspaceId)){
                        simpMessagingTemplate.convertAndSendToUser(subscription.getRequestId(), "/topic/messages", message );
                    }
                }
            }
        } catch (Exception e){
            log.error("Unable to send notification. {}", e.getMessage(), e.getCause());
        }
    }


    @Async
    @Override
    public void sendNotifications(List<String> targetUsers, String source, String sourceId, String workspaceId, String spaceId, String message) {
        if(Objects.isNull(targetUsers) || targetUsers.isEmpty()){
            return;
        }
        List<NotificationData> notifications = new ArrayList<>();
        for(String user : targetUsers){
            notifications.add(new NotificationData(user, source, sourceId, workspaceId, spaceId, message));
        }
        notificationsRepository.saveAll(notifications);
        for(NotificationData notification : notifications){
            send(notification.getUserId(), notification.getWorkspaceId(), notification);
        }
    }

    @Override
    public ResponseEntity<String> unsubscribe(String id) {
        String user = userService.getCurrentUserId();
        if(users.containsKey(user)){
            users.get(user).removeIf(u-> Objects.equals(u.getRequestId(), id));
        }
        return new ResponseEntity<>("Unsubscribed", HttpStatus.OK);
    }
}
