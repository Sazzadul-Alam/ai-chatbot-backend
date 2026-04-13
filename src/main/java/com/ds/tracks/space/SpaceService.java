package com.ds.tracks.space;

import com.ds.tracks.space.model.SubSpace;
import com.ds.tracks.space.model.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface SpaceService {
    ResponseEntity<?> create(SpaceDto space, HttpServletRequest request);
    ResponseEntity<?> saveSubspace(SubSpace subSpace, HttpServletRequest request);
    ResponseEntity<?> getSpaceWiseUsers(String id);

    ResponseEntity<?> invite(String workSpaceId, String spaceId, String email, HttpServletRequest request);

    ResponseEntity<?> changeRole(String role,String spaceId, String id);

    ResponseEntity<?> changeDesignation(String designation,String spaceId, String id);


    ResponseEntity<?> getConfig(String spaceId, String subSpaceId, String param);

    ResponseEntity<?> configureWeekends(WeekendConfigDto configDto);


    ResponseEntity<?> configureWorkHour(String workspaceId, String spaceId, String source, Double duration);


    Object getWorkConfigurations(String spaceId);

    ResponseEntity<?> createFolder(FolderDto folderDto, HttpServletRequest request);

    ResponseEntity<?> subSpaceWithoutFolder(String spaceId, String folderId);

    ResponseEntity<?> getFolder(String id);

    Map<String, Object> getHolidaysAndWeekend(String spaceId);

    ResponseEntity<?> get(String spaceId, String subspaceId);

    ResponseEntity<?> update(SpaceDto space, HttpServletRequest request);

    String getFinalStage(String spaceId, String subspaceId);


    void saveTags(String spaceId, List<String> tags);

    ResponseEntity<?> getList(String workspaceId);

    ResponseEntity<?> getSubspaceList(String spaceId);

    ResponseEntity<?> segmentList(String spaceId);

    ResponseEntity<?>  segments(String spaceId);

    ResponseEntity<?>  projects(String workspaceId);

    ResponseEntity<?> project(String spaceId);


    ResponseEntity<?> userDetails();


    ResponseEntity<String> updateMenu(String id, List<String> menu);

    ResponseEntity<?> getPermission(String workspaceId);



    ResponseEntity<?> deleteSpace(String id);

    ResponseEntity<?> deleteSegment(String id);

    ResponseEntity<?> deleteFolder(String id);

    ResponseEntity<?> revoke(String id, String space, String workspace);

    ResponseEntity<?> getTags(String workspaceId);
    ResponseEntity<?> saveTag(String workspaceId, String tag);
    ResponseEntity<?> removeTag(String workspaceId, String tag);

    ResponseEntity<?> getUserList( String space);

    ResponseEntity<?> editConfiguration(String workspace, String space, String subspace, String source, String param, String action);

    ResponseEntity<?> getImage(String id);

    ResponseEntity<?> initializeCategory(SpaceCategoryDto spaceCategoryDto);

    ResponseEntity<?> fileUpload(String id, String type, MultipartFile file);

    ResponseEntity<?> fileList(String id);

    ResponseEntity<?> fileDelete(String id);

    void fileDownload(String id, HttpServletResponse response);

    ResponseEntity<?> updateType(String spaceId, String type);

    ResponseEntity<?> findListOfUserFullNames(String id);

    ResponseEntity<?> inviteBulk(String space, List<String> userIds);
}
