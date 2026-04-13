package com.ds.tracks.space.controller;

import com.ds.tracks.space.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/space/files")
@RequiredArgsConstructor
public class SpaceFilesController {

    private final SpaceService spaceService;

    @PostMapping("/list")
    public ResponseEntity<?> fileList(@RequestParam String id){
        return spaceService.fileList(id);
    }
    @PostMapping("/upload")
    public ResponseEntity<?> fileUpload(@RequestParam String id, @RequestParam String type, @RequestParam MultipartFile file){
        return spaceService.fileUpload(id, type, file);
    }
    @PostMapping("/download")
    public void fileDownload(@RequestParam String id, HttpServletResponse response){
        spaceService.fileDownload(id, response);
    }
    @PostMapping("/delete")
    public ResponseEntity<?> fileDelete(@RequestParam String id){
        return spaceService.fileDelete(id);
    }
}
