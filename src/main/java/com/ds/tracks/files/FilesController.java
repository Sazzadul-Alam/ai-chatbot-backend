package com.ds.tracks.files;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FilesController {
    private final FilesService filesService;
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam String workspaceId,
            @RequestParam String spaceId,
            @RequestParam(required = false) String subspaceId,
            @RequestParam MultipartFile file,
            @RequestParam String source,
            @RequestParam("id") String sourceId){
        return filesService.upload(workspaceId, spaceId, subspaceId, file, source, sourceId);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete( @RequestParam String id){
        return filesService.delete(id);
    }


    @PostMapping("/getAll")
    public ResponseEntity<?> getAll(
            @RequestParam String source,
            @RequestParam("id") String sourceId){
        return filesService.getAll(sourceId, source);
    }

    @PostMapping("/download")
    public ResponseEntity<?> download(@RequestParam String id, HttpServletResponse response){
        return filesService.download(id, response);
    }
}
