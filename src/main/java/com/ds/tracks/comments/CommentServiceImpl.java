package com.ds.tracks.comments;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;

import static com.ds.tracks.commons.utils.Utils.isValidString;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService{
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final AuditLogService auditLogService;

    @Override
    public ResponseEntity<?> comment(Comment comment) {
        if(isValidString(comment.getSourceId()) && isValidString(comment.getSource())
                && isValidString(comment.getWorkspaceId()) && isValidString(comment.getSourceId())){
            comment.setId(null);
            comment.setUserId(userService.getCurrentUserId());
            comment.setDate(new Date());
            comment.setSubspaceId(isValidString(comment.getSubspaceId()) ? comment.getSubspaceId() : null);
            commentRepository.save(comment);
            auditLogService.save("Commented", comment.getSource(), comment.getSourceId(), comment.getSpaceId(), comment.getSubspaceId());
            return new ResponseEntity<>("Commented", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> fetch(String id, String type) {
        auditLogService.save("Viewed Comments", type, id);
        return new ResponseEntity<>(commentRepository.fetch(userService.getCurrentUserId(), id, type), HttpStatus.OK);
    }
}
