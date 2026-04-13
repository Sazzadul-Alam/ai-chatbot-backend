package com.ds.tracks.space.repository;

import com.ds.tracks.holiday.model.Holiday;
import com.ds.tracks.space.model.dto.SpaceDto;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface SpaceDao {

    List<Map>  getSpacewiseUsers(String spaceId);
    List<Map<String, Object>>  getUserPermissionsForSpace(String spaceId);
    UpdateResult configureWeekends(String workspaceId, String spaceId, String source, List<String> weekends);

    UpdateResult configureWorkHour(String workspaceId, String spaceId, String source, Double duration);

    Object getWorkConfigurations(String spaceId);

    UpdateResult putSubSpacesIntoFolder(List<String> subspaces, String id);


    UpdateResult removeFolderFromSubspaces(List<String> unmapped);

    UpdateResult saveHoliday(List<Holiday> holidayList, String spaceId);
    UpdateResult saveWorkspaceHoliday(List<Holiday> holidays, String workspaceId);

    Map<String, Object> getDetailsWithConfig(String id, Boolean forSpace);

    void update(SpaceDto space);


    String getFinalStage(String spaceId, String subspaceId);

    Map<String, Object> getConfigurationsForTask(String spaceId, Boolean subspaceId);


    void updateTag(String spaceId, List<String> tags);

    List<?> getList(String currentUserId, String workspaceId);

    Object getSubspaceList(String spaceId);

    List<?> getSegmentsList(String spaceId);
    List<Document> getSegmentsByFolderId(String folderId);

    Object segments(String spaceId);

    Object projects(String workspaceId, String currentUserId);

    Object allProjects(String workspaceId);

    Object project(String spaceId, String currentUserId, boolean isSuperAdmin);

    Object availableWorkspaces(String currentWorkspace, String currentUserId);


    Object getWorkspaceDetailsForConfig(String id);


    List<String> getAllProjectsUserNotAddedIn(String userId, String workspaceId);

    void updateMenu(String spaceId, List<String> menu);

    Object getConfigs();

    String getFolderId(String subSpaceId);

    List<String> getSubspacesByFolderId(String subSpaceId);


    Document findPrimaryWorkspace(String name);

    void deleteSegments(List<String> asList);

    void deleteSpace(List<String> id);

    void deleteWorkspace(String id);

    List<String> getStages(String subspaceId);

    void addDesignation(String id, String designation);
    void removeDesignation(String id, String designation);
    void editDesignation(String id, String designation, String newDesignation);

    void removeWorkspaceSuperuser(String workspaceId, String userId);

    void removeFromSpace(String workspace, String space, String id);

    List<String> getTags(String workspaceId);
    void saveTag(String tag, String workspaceId);
    List<Document> getUserListForSpace(String space);

    void editSegmentConfiguration(String subspace, String source, String param, String action);

    Object getProjectsInWorkspace(String id);

    Object findWorkspaceHolidays(String id);

    List<String> findAllSpaces(String workspaceId);

    void incrementWorkspaceInvoice(String workspaceId);

    void addWorkspaceTaskConfig(String type, String value, String id);

    void removeWorkspaceTaskConfig(String type, String value, String id);

    void updateWorkspaceStages(String id, List<String> stages);

    void addCategoryAndAddSegmentsToFolder(String spaceId, String category, String folderId, List<String> collect);

    Object getFileList(String id);

    void updateType(String spaceId, String type);

    Object findListOfUserFullNames(String id);
}
