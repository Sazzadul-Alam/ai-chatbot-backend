package com.ds.tracks.space.controller;

import com.ds.tracks.space.SpaceService;
import com.ds.tracks.space.model.dto.FolderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/space/folder")
@RequiredArgsConstructor
public class SpaceFoldersController {

    private final SpaceService spaceService;

    @PostMapping("/details")
    public ResponseEntity<?> getFolder(@RequestParam String id){
        return spaceService.getFolder(id);
    }
    @PostMapping("/save")
    public ResponseEntity<?> createFolder(@RequestBody FolderDto folderDto, HttpServletRequest request){
        return spaceService.createFolder(folderDto, request);
    }
    @PostMapping("/delete")
    public ResponseEntity<?> deleteFolder( @RequestParam String id){
        return spaceService.deleteFolder(id);
    }
}
