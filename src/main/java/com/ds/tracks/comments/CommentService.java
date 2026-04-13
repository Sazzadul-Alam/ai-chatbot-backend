package com.ds.tracks.comments;

import org.springframework.http.ResponseEntity;

public interface CommentService {
    ResponseEntity<?> comment(Comment comment);

    ResponseEntity<?> fetch(String id, String type);
}
