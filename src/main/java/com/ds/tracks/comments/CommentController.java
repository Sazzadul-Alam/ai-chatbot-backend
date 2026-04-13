package com.ds.tracks.comments;

import com.ds.tracks.backlog.Backlog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/add")
    public ResponseEntity<?> comment(@RequestBody Comment comment){
        return commentService.comment(comment);
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetch(@RequestParam String id, @RequestParam String type){
        return commentService.fetch(id, type);
    }

}
