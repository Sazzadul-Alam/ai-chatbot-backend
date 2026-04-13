package com.ds.tracks.space;


import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.models.enums.ActiveStatus;
import com.ds.tracks.commons.models.enums.ManagementRoles;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import com.ds.tracks.files.FilesService;
import com.ds.tracks.holiday.model.Holiday;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.space.model.*;
import com.ds.tracks.space.model.dto.*;
import com.ds.tracks.space.repository.*;
import com.ds.tracks.tasks.model.TaskDraft;
import com.ds.tracks.tasks.repository.TaskDraftRepository;
import com.ds.tracks.template.*;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.UsersPermission;
import com.ds.tracks.user.repository.UsersPermissionRepository;
import com.ds.tracks.user.service.UserService;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.print.Doc;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.models.enums.ManagementRoles.ADMIN;
import static com.ds.tracks.commons.models.enums.ManagementRoles.OBSERVER;
import static com.ds.tracks.commons.models.enums.PermissionLayer.*;
import static com.ds.tracks.commons.utils.Utils.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceServiceImpl implements SpaceService {
    private final SpaceRepository spaceRepository;
    private final SpaceDao spaceDao;
    private final TemplateDao templateDao;
    private final SubSpaceRepository subSpaceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UsersPermissionRepository usersPermissionRepository;
    private final UserService userService;
    private final FolderRepository folderRepository;
    private final CustomTemplateRepository templateRepository;
    private final TaskDraftRepository draftRepository;
    private final CustomTemplateTasksRepository templateTaskRepository;
    private final FilesService filesService;
    private final AuditLogService auditLogService;

    @Override
    public ResponseEntity<?> create(SpaceDto spaceDto, HttpServletRequest request) {
        if(userService.hasAccess(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT))){
            try {
                if(Objects.isNull(spaceDto.getStartDate())){
                 spaceDto.setStartDate(new Date());
                }
                if(Objects.isNull(spaceDto.getEndDate())){
                 spaceDto.setEndDate(spaceDto.getStartDate());
                }
                spaceDto.getStartDate().setHours(6);
                spaceDto.getEndDate().setHours(6);
                String image = null;
                String currentUserId = userService.getCurrentUserId();
                if(isValidString(spaceDto.getImage())){
                    image = spaceDto.getImage().split(",")[1];
                }
                String id = spaceRepository.save(Space.builder()
                        .workspaceId(spaceDto.getWorkspaceId())
                        .createdAt(new Date())
                        .createdBy(currentUserId)
                        .plannedStartDate(spaceDto.getStartDate())
                        .plannedEndDate(spaceDto.getEndDate())
                        .status(ActiveStatus.ACTIVE)
                        .name(spaceDto.getName())
                        .phone(spaceDto.getPhone())
                        .address(spaceDto.getAddress())
                        .mnemonic(spaceDto.getMnemonic())
                        .image(image)
                        .clientType(Objects.isNull(spaceDto.getClientType()) ? "Normal": spaceDto.getClientType())
                        .description(spaceDto.getDescription())
                        .color(spaceDto.getColor())
                        .color(spaceDto.getColor())
                        .menus(spaceDto.getMenus())
                        .configurations(
                                SpaceConfigurations.builder()
                                        .lockStatus(spaceDto.getLockStage())
                                        .workHour(8D)
                                        .category(spaceDto.getCategory())
                                        .status(spaceDto.getStatus())
                                        .type(spaceDto.getTypes())
                                        .build()
                        )
                        .build()).getId();
                String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
                List<UsersPermission> permissions = usersPermissionRepository.findAllByWorkspaceIdAndPermissionFor(spaceDto.getWorkspaceId(), WORKSPACE);
                if(Objects.isNull(permissions)){
                    permissions = new ArrayList<>();
                }
                Date now = new Date();
                permissions.add(UsersPermission.builder()
                        .userId(currentUserId)
                        .loginId(loginId)
                        .permissionFor(SPACE)
                        .role(OBSERVER)
                        .spaceId(id)
                        .workspaceId(spaceDto.getWorkspaceId())
                        .createdBy(currentUserId)
                        .createdAt(now).build());
                for(UsersPermission permission : permissions){
                    if(!Objects.equals(permission.getUserId(), currentUserId)){
                        permission.setId(null);
                        permission.setWorkspaceId(spaceDto.getWorkspaceId());
                        permission.setSpaceId(id);
                        permission.setRole(OBSERVER);
                        permission.setPermissionFor(SPACE);
                        permission.setCreatedAt(now);
                        permission.setCreatedBy(currentUserId);
                    }
                }
                usersPermissionRepository.saveAll(permissions);
                auditLogService.save("Created Client", CollectionName.spaces, id, id, null);
                return new ResponseEntity<>(new Document("id", id).append("name", spaceDto.getName()), OK);
            } catch (Exception e) {
                return exception();
            }
        }
        return forbidden();
    }
    @Override
    public ResponseEntity<?> update(SpaceDto spaceDto, HttpServletRequest request) {
        if(Objects.nonNull(spaceDto) && isValidString(spaceDto.getId())){
            Space space = spaceRepository.findFirstById(spaceDto.getId());
            if(Objects.nonNull(space)){
                space.setName(spaceDto.getName());
                space.setMnemonic(spaceDto.getMnemonic());
                space.setAddress(spaceDto.getAddress());
                space.setColor(spaceDto.getColor());
                spaceDto.getStartDate().setHours(6);
                spaceDto.getEndDate().setHours(6);
                space.setPlannedStartDate(spaceDto.getStartDate());
                space.setPlannedEndDate(spaceDto.getEndDate());
                space.setPhone(spaceDto.getPhone());
                space.setCategories(spaceDto.getClientCategories());
                space.setClientType(spaceDto.getClientType());
                if(Objects.isNull(space.getConfigurations().getHistoricCategory())){
                    space.getConfigurations().setHistoricCategory(new ArrayList<>());
                }
                if(Objects.isNull(space.getConfigurations().getHistoricType())){
                    space.getConfigurations().setHistoricType(new ArrayList<>());
                }
                if(Objects.isNull(space.getMenus())){
                    space.setMenus(new ArrayList<>());
                }
                if(Objects.nonNull(spaceDto.getRemoveCategories()) && !spaceDto.getRemoveCategories().isEmpty()){
                    space.getConfigurations().getCategory().removeAll(spaceDto.getRemoveCategories());
                    space.getConfigurations().getHistoricCategory().addAll(spaceDto.getRemoveCategories());
                }
                if(Objects.nonNull(spaceDto.getRemoveTypes()) && !spaceDto.getRemoveTypes().isEmpty()){
                    space.getConfigurations().getType().removeAll(spaceDto.getRemoveTypes());
                    space.getConfigurations().getHistoricType().addAll(spaceDto.getRemoveTypes());
                }
                if(Objects.nonNull(spaceDto.getCategory()) && !spaceDto.getCategory().isEmpty()){
                    space.getConfigurations().getCategory().addAll(spaceDto.getCategory());
                }
                if(Objects.nonNull(spaceDto.getTypes()) && !spaceDto.getTypes().isEmpty()){
                    space.getConfigurations().getType().addAll(spaceDto.getTypes());
                }
                if(Objects.nonNull(spaceDto.getStatus()) && !spaceDto.getStatus().isEmpty()){
                    space.getConfigurations().getStatus().addAll(spaceDto.getStatus());
                }
                if(isValidString(spaceDto.getImage())){
                    String image = spaceDto.getImage().split("base64,")[1];
                    space.setImage(image);
                } else {
                    space.setImage(null);
                }
                spaceRepository.save(space);
                auditLogService.save("Updated Client", CollectionName.spaces, space.getId(), space.getId(), null);
                return ResponseEntity.ok("Updated");
            }
        }
        return badRequest();
    }
    @Override
    public ResponseEntity<?> saveSubspace(SubSpace subSpace, HttpServletRequest request) {
        if(userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), subSpace.getSpaceId())){
            if(isValidString(subSpace.getId())){
                SubSpace savedSubSpace = subSpaceRepository.findFirstById(subSpace.getId()).orElse(null);
                if(Objects.nonNull(savedSubSpace)){
                    savedSubSpace.setName(subSpace.getName());
                    savedSubSpace.setColor(subSpace.getColor());
                    savedSubSpace.setPlannedStartDate(subSpace.getPlannedStartDate());
                    savedSubSpace.setPlannedEndDate(subSpace.getPlannedEndDate());
                    savedSubSpace.setUpdatedBy(userService.getCurrentUserId());
                    savedSubSpace.setUpdatedAt(new Date());
                    auditLogService.save("Updated Segment", CollectionName.sub_spaces, savedSubSpace.getId(), savedSubSpace.getSpaceId(), savedSubSpace.getId());
                    subSpaceRepository.save(savedSubSpace);
                    return new ResponseEntity<>(new Document("id", subSpace.getId()).append("name", subSpace.getName()), OK);
                } else {
                    return badRequest();
                }
            } else {
                if(Objects.isNull(subSpace.getPlannedStartDate())){
                    subSpace.setPlannedStartDate(new Date());
                }
                if(Objects.isNull(subSpace.getPlannedEndDate())){
                    subSpace.setPlannedEndDate(new Date());
                }
                subSpace.setCreatedAt(new Date());
                subSpace.setCreatedBy(userService.getCurrentUserId());
                subSpaceRepository.save(subSpace);
                auditLogService.save("Created Segment", CollectionName.sub_spaces, subSpace.getId(), subSpace.getSpaceId(), subSpace.getId());
                return new ResponseEntity<>(new Document("id", subSpace.getId()).append("name", subSpace.getName()), OK);
            }
        }
        return forbidden();

    }

    @Override
    public ResponseEntity<?> createFolder(FolderDto folderDto, HttpServletRequest request) {
        if(userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), folderDto.getSpaceId())){
            Folder folder = null;
            if(isValidString(folderDto.getId())){
                folder = folderRepository.findById(folderDto.getId()).orElse(new Folder());
                folder.setName(folderDto.getName());
                folder.setCategory(folderDto.getCategory());
                if(!Objects.equals(folderDto.getApp(), Boolean.TRUE)){
                    folder.setPlannedStartDate(folderDto.getStartDate());
                    folder.setPlannedEndDate(folderDto.getEndDate());
                }
                List<String> unmapped = new ArrayList<>();
                if(Objects.nonNull(folder.getSubspaces()) && Objects.nonNull(folderDto.getSubspaces())){
                    for(String subspace : folder.getSubspaces()) {
                        if(!folderDto.getSubspaces().contains(subspace)){
                            unmapped.add(subspace);
                        }
                    }
                }
                folder.setSubspaces(folderDto.getSubspaces());
                if(!unmapped.isEmpty()){
                    spaceDao.removeFolderFromSubspaces(unmapped);
                }
            } else {
                if(Objects.isNull(folderDto.getStartDate())){
                    folderDto.setStartDate(new Date());
                }
                folder = Folder.builder()
                        .name(folderDto.getName())
                        .plannedStartDate(folderDto.getStartDate())
                        .plannedEndDate(Objects.isNull(folderDto.getEndDate()) ? folderDto.getStartDate(): folderDto.getEndDate())
                        .subspaces(folderDto.getSubspaces())
                        .workspaceId(folderDto.getWorkspaceId())
                        .spaceId(folderDto.getSpaceId())
                        .build();
            }
            if(Objects.nonNull(folder)) {
                String action = Objects.nonNull(folder.getId()) ? "Updated Folder" : "Created Folder";
                String id = folderRepository.save(folder).getId();
                auditLogService.save( action, CollectionName.folder, folder.getId(), folder.getSpaceId(), null);
                if(Objects.nonNull(folderDto.getSubspaces()) && !folderDto.getSubspaces().isEmpty() && isValidString(id)) {
                    spaceDao.putSubSpacesIntoFolder(folderDto.getSubspaces(), id);
                }
            }
            return new ResponseEntity<>("Saved", OK);
        }
        return forbidden();
    }

    @Override
    public ResponseEntity<?> invite(String workSpaceId, String spaceId, String email, HttpServletRequest request) {
        if(userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), spaceId)){
            if (isValidEmail(email)) {
                UsersPermission permission = usersPermissionRepository.findOneBySpaceIdAndLoginId(spaceId, email);
                if(Objects.nonNull(permission) && Objects.equals(permission.getRole(), OBSERVER)){
                    permission.setRole(ADMIN);
                    usersPermissionRepository.save(permission);
                    auditLogService.save("Added  "+email+" to Client", CollectionName.user, permission.getUserId(), permission.getSpaceId(), null);
                    return new ResponseEntity<>(Collections.emptyList(), OK);
                } else if(Objects.isNull(permission)){
                    String userId = userService.findUsersByLoginIds(Arrays.asList(email)).getOrDefault(email, null);
                    if(isValidString(userId)){
                        permission = UsersPermission.builder()
                                .createdAt(new Date())
                                .createdBy(userService.getCurrentUserId())
                                .spaceId(spaceId)
                                .workspaceId(workSpaceId)
                                .loginId(email.trim())
                                .userId(userId)
                                .permissionFor(SPACE)
                                .role(ManagementRoles.USER).build();
                        usersPermissionRepository.save(permission);
                        auditLogService.save("Added  "+email+" to Client", CollectionName.user, userId, permission.getSpaceId(), null);

                        return new ResponseEntity<>(Collections.emptyList(), OK);
                    }
                    return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
                }
                return new ResponseEntity<>("User already invited", HttpStatus.BAD_REQUEST);
            }
            return badRequest();
        }
        return forbidden();
    }
    @Override
    public ResponseEntity<?> changeRole(String role,String spaceId, String id) {
        if (isValidString(role)) {
            auditLogService.save("Changed user role for client", CollectionName.user, id, spaceId, null);
            UpdateResult updateResult = usersPermissionRepository.changeRole(id, spaceId, role);
            if (updateResult.wasAcknowledged()) {
                return new ResponseEntity<>("Updated", OK);
            }
        }
        return badRequest();
    }
    @Override
    public ResponseEntity<?> changeDesignation(String designation,String spaceId, String id) {
        if(userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), spaceId)){
            if (isValidString(designation)) {
                UpdateResult updateResult = usersPermissionRepository.changeDesignation(id, spaceId, designation);
                if (updateResult.wasAcknowledged()) {
                    return new ResponseEntity<>("Updated", OK);
                }
            }
            return badRequest();
        }
        return unauthorized();
    }

    @Override
    public ResponseEntity<?> getConfig(String spaceId, String subspaceId, String param) {
        boolean forSubspace = isValidString(subspaceId);
        String id = forSubspace ? subspaceId : spaceId;
        if(isValidString(id)){
            Map<String, Object> res = spaceDao.getConfigurationsForTask(id, forSubspace);
            if(Objects.nonNull(res)){
                if(Objects.nonNull(res.get("type"))){
                    Collections.sort((List<String>)res.get("type"));
                }
                if(Objects.nonNull(res.get("category"))){
                    Collections.sort((List<String>)res.get("category"));
                }
            }
            return new ResponseEntity<>(res, OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> configureWeekends(WeekendConfigDto configDto) {
        try {
            List<String> selectedDays = new ArrayList<>();
            if (Objects.nonNull(configDto.getWeekdays())) {
                for (String day : configDto.getWeekdays().keySet()) {
                    if (configDto.getWeekdays().get(day)){
                        selectedDays.add(day);
                    }
                }
            }
            UpdateResult updateResult = spaceDao.configureWeekends(configDto.getWorkspaceId(), configDto.getSpaceId(), configDto.getSource(), selectedDays);
            if(updateResult.wasAcknowledged()){
                return new ResponseEntity<>("Weekends recorded", OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch ( Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> configureWorkHour(String workspaceId, String spaceId, String source, Double duration) {
        try {
            UpdateResult updateResult = spaceDao.configureWorkHour(workspaceId, spaceId,source, duration);
            if(updateResult.wasAcknowledged()){
                return new ResponseEntity<>("Work-hour recorded", OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch ( Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Object getWorkConfigurations(String spaceId) {
        return spaceDao.getWorkConfigurations(spaceId);
    }



    @Override
    public ResponseEntity<?> subSpaceWithoutFolder(String spaceId, String folderId) {
       List<SubSpace> subSpaces = subSpaceRepository.findAllBySpaceIdAndFolderIdIsNull(spaceId);
       if(isValidString(folderId)){
           List<Document> documents = spaceDao.getSegmentsByFolderId(folderId);
           if(Objects.isNull(documents)){
               documents = new ArrayList<>();
           }
           for(SubSpace subSpace: subSpaces){
               documents.add(new Document("id", subSpace.getId()).append("name", subSpace.getName()).append("color", subSpace.getColor()));
           }
           return new ResponseEntity<>(documents, OK);
       }

        return new ResponseEntity<>(subSpaces, OK);
    }

    @Override
    public ResponseEntity<?> getFolder(String id) {
        Folder folder = folderRepository.findById(id).orElse(null);
        if(Objects.nonNull(folder)){
            auditLogService.save("Viewed Folder Details", CollectionName.folder, id, folder.getSpaceId(), null);
        }
        return new ResponseEntity<>(folder, OK);
    }

    @Override
    public Map<String, Object> getHolidaysAndWeekend(String workspaceId) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Boolean> weekDays = new LinkedHashMap<>();
        weekDays.put("Monday", false);
        weekDays.put("Tuesday", false);
        weekDays.put("Wednesday", false);
        weekDays.put("Thursday", false);
        weekDays.put("Friday", false);
        weekDays.put("Saturday", false);
        weekDays.put("Sunday", false);
        Workspace workspace = workspaceRepository.findFirstConfiguration(workspaceId);
        SpaceConfigurations configuration = Objects.nonNull(workspace) ? workspace.getConfigurations() : null;
        if(Objects.nonNull(configuration)){
            if(Objects.nonNull(configuration.getWeekend())){
                configuration.getWeekend().forEach(day->weekDays.put(day, true));
            }
            Map<String, List<String>> holidays = new HashMap<>();
            if(Objects.nonNull(configuration.getHoliday())){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                for(Holiday day : configuration.getHoliday()) {
                    if(holidays.containsKey(sdf.format(day.getDate()))){
                        holidays.get(sdf.format(day.getDate())).add(day.getEventName());
                    } else {
                        List<String> holiday = new ArrayList<>();
                        holiday.add(day.getEventName());
                        holidays.put(sdf.format(day.getDate()), holiday);
                    }
                }
            }
            response.put("holiday", holidays);
        }
        response.put("weekend", weekDays);
        return response;
    }



    @Override
    public ResponseEntity<?> getSpaceWiseUsers(String spaceId) {
        return new ResponseEntity<>(spaceDao.getSpacewiseUsers(spaceId), OK);
    }


    @Override
    public ResponseEntity<?> get(String spaceId, String subspaceId) {
        Boolean forSpace = !isValidString(subspaceId);
        if(forSpace){
            auditLogService.save("Viewed Client Details", CollectionName.spaces, spaceId, spaceId,  null);
        } else {
            auditLogService.save("Viewed Segment Details", CollectionName.sub_spaces, subspaceId, spaceId,  subspaceId);
        }
        Map<String, Object> res = spaceDao.getDetailsWithConfig((forSpace ?  spaceId : subspaceId), forSpace);
        if(Objects.nonNull(res)){
            return new ResponseEntity<>(res, OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }



    @Override
    public String getFinalStage(String spaceId, String subspaceId) {
        try{
            return spaceDao.getFinalStage(spaceId, subspaceId);
        } catch (Exception ignored){}
        return null;
    }



    @Override
    public void saveTags(String spaceId, List<String> tags) {
        spaceDao.updateTag(spaceId, tags);
    }

    @Override
    public ResponseEntity<?> getList(String workspaceId) {
        return new ResponseEntity<>(spaceDao.getList(userService.getCurrentUserId(), workspaceId ), OK);
    }

    @Override
    public ResponseEntity<?> getSubspaceList(String spaceId) {
        return new ResponseEntity<>(spaceDao.getSubspaceList(spaceId), OK);
    }

    @Override
    public ResponseEntity<?> segmentList(String spaceId) {
        return new ResponseEntity<>(spaceDao.getSegmentsList(spaceId), OK);
    }

    @Override
    public ResponseEntity<?> segments(String spaceId) {
        return new ResponseEntity<>(spaceDao.segments(spaceId), OK);
    }

    @Override
    public ResponseEntity<?> projects(String workspaceId) {
        if(userService.hasAccess(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT))){
            return new ResponseEntity<>(spaceDao.allProjects(workspaceId),OK);
        }
        return new ResponseEntity<>(spaceDao.projects(workspaceId, userService.getCurrentUserId()), OK);
    }

    @Override
    public ResponseEntity<?> project(String spaceId) {
        Object space = spaceDao.project(spaceId, userService.getCurrentUserId(), userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), spaceId));
        if(Objects.isNull(space)){
            return Utils.unauthorized();
        } else {
            return new ResponseEntity<>(space, OK);
        }
    }



    @Override
    public ResponseEntity<?> userDetails() {
        return new ResponseEntity<>(userService.getUserInfo(), OK);
    }

    @Override
    public ResponseEntity<String> updateMenu(String id, List<String> menu) {
        this.spaceDao.updateMenu(id, menu);
        return new ResponseEntity<>("Updated", OK);
    }

    @Override
    public ResponseEntity<?> getPermission(String workspaceId) {
//        return new ResponseEntity<>(userService.isSuperAdmin(workspaceId) ? workspaceId : null, OK);
        return null;
    }

    @Override
    public ResponseEntity<?> deleteSpace(String id) {
        if(userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), id)){
            try {
                Space space = spaceRepository.findFirstById(id);
                List<String> ids = subSpaceRepository.findAllIdBySpaceId(id).stream().map(SubSpace::getId).collect(Collectors.toList());
                auditLogService.logMultipleDeletedSources("Deleted Client", CollectionName.spaces, id, id, ids, space.getName());
                spaceDao.deleteSpace(Arrays.asList(id));
                return new ResponseEntity<>("Space Deleted", OK);
            } catch (Exception e){
                log.error(e.getMessage(), e);
                return exception();
            }
        }
        return unauthorized();
    }

    @Override
    public ResponseEntity<?> deleteSegment(String id) {
        if(isValidString(id)){
            SubSpace subSpace = subSpaceRepository.findSpaceId(id);
            if(Objects.nonNull(subSpace) && userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), id)){
                try {
                    auditLogService.logDeletedSource("Deleted Segment", CollectionName.sub_spaces, id, subSpace.getSpaceId(), id, subSpace.getName());
                    spaceDao.deleteSegments(Arrays.asList(id));
                    return new ResponseEntity<>("Segment Deleted", OK);
                } catch (Exception e){
                    log.error(e.getMessage(), e);
                    return exception();
                }
            }
            return unauthorized();
        }
        return badRequest();
    }

    @Override
    public ResponseEntity<?> deleteFolder(String id) {
        if(isValidString(id)){
            Folder folder = folderRepository.findSpaceAndSubspaces(id);
            if(Objects.nonNull(folder) && userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), folder.getSpaceId())){
                try {
                    auditLogService.logMultipleDeletedSources("Deleted Folder", CollectionName.folder, id, folder.getSpaceId(), folder.getSubspaces(), folder.getName());
                    spaceDao.deleteSegments(folder.getSubspaces());
                    folderRepository.deleteById(id);
                    return new ResponseEntity<>("Folder Deleted", OK);
                } catch (Exception e){
                    log.error(e.getMessage(), e);
                    return exception();
                }
            }
            return unauthorized();
        }
        return badRequest();
    }

    @Override
    public ResponseEntity<?> revoke(String id, String space, String workspace) {
        if(isValidString(space)){
            try {
                auditLogService.save("Removed user from client", CollectionName.user, id, space, null);
                spaceDao.removeFromSpace(workspace, space, id);
                return new ResponseEntity<>("User removed", OK);
            } catch (Exception e){
                log.error(e.getMessage(), e.getCause());
                return exception();
            }
        }
        return badRequest();
    }


    @Override
    public ResponseEntity<?> getTags(String workspaceId) {
        return new ResponseEntity<>(spaceDao.getTags(workspaceId), OK);
    }
    @Override
    public ResponseEntity<?> saveTag(String id, String tag) {
        spaceDao.saveTag(tag, id);
        return new ResponseEntity<>("Saved",OK);
    }
    @Override
    public ResponseEntity<?> removeTag(String id, String tag) {
        spaceDao.saveTag(tag, id);
        return new ResponseEntity<>("Removed",OK);
    }

    @Override
    public ResponseEntity<?> getUserList( String space) {
        auditLogService.save("Viewed client invited user list", CollectionName.spaces, space, space, null);
        return new ResponseEntity<>(spaceDao.getUserListForSpace(space), OK);
    }

    @Override
    public ResponseEntity<?> editConfiguration(String workspace, String space, String subspace, String source, String param, String action) {
        if(userService.hasAccessOrIsClientAdmin(Arrays.asList(AccessPoints.CLIENT_MANAGEMENT), space)){
            spaceDao.editSegmentConfiguration(subspace, source, param, action);

        }
        return unauthorized();
    }

    @Override
    public ResponseEntity<?> getImage(String id) {
        Space space = spaceRepository.findFirstImageById(id);

        return new ResponseEntity<>(Objects.nonNull(space) ? space.getImage() : null, OK);
    }

    @Override
    public ResponseEntity<?> initializeCategory(SpaceCategoryDto spaceCategory) {
        if(Objects.nonNull(spaceCategory.getSpaceId()) && Objects.nonNull(spaceCategory.getCategory())){
            Folder folder = folderRepository.findBySpaceIdAndName(spaceCategory.getSpaceId(), spaceCategory.getCategory());
            String folderId = Objects.nonNull(folder) ? folder.getId() : null;
            if(Objects.isNull(folderId)){
                folder = new Folder();
                folder.setSpaceId(spaceCategory.getSpaceId());
                folder.setName(spaceCategory.getCategory());
                folder.setCategory(spaceCategory.getCategory());
                folder.setSubspaces(new ArrayList<>());
                folderId = folderRepository.save(folder).getId();
            }
            List<Map<String, Object>> templates =  templateDao.findTemplatesByTasks(spaceCategory.getSelectedTasks());
            HashSet<String> templateIds = new HashSet<>();
            if(Objects.nonNull(spaceCategory.getTemplates()) && !spaceCategory.getTemplates().isEmpty()){
                templateIds.addAll(spaceCategory.getTemplates());
            }
            if(!templates.isEmpty()){
                templates.forEach(template-> templateIds.add(template.get("id").toString()));
            }
            Date today = new Date();
            List<CustomTemplate> templateList = templateRepository.findAllByIdIn(new ArrayList<>(templateIds));
            String userId = userService.getCurrentUserId();
            Map<String, String> templateRef = new HashMap<>();
            for(CustomTemplate template : templateList){
                SubSpace subSpace = new SubSpace();
                subSpace.setSpaceId(spaceCategory.getSpaceId());
                subSpace.setName(template.getName());
                subSpace.setFolderId(folderId);
                subSpace.setCreatedAt(today);
                subSpace.setColor("#5c2e91");
                subSpace.setCreatedBy(userId);
                subSpace.setPlannedStartDate(today);
                subSpace.setPlannedEndDate(today);
                subSpaceRepository.save(subSpace); // Note: Saving inside the loop instead of using saveall for a reason. Dont optimize
                templateRef.put(template.getId(), subSpace.getId());
            }
            String stage = "Todo" ;
            List<CustomTemplateTasks> tasksListDb = templateTaskRepository.findAllByTemplateIdInOrderByPosition(new ArrayList<>(templateIds));
            HashSet<String> ids = new HashSet<>();
            if(Objects.nonNull(spaceCategory.getSelectedTasks())){
                ids.addAll(spaceCategory.getSelectedTasks());
            }
            List<CustomTemplateTasks> tasksList = tasksListDb.stream()
                    .filter(task ->ids.contains(task.getId()))
                    .collect(Collectors.toList());;
            List<TaskDraft> drafts = new ArrayList<>();
            for(CustomTemplateTasks task : tasksList){
                drafts.add(TaskDraft.builder()
                        .spaceId(spaceCategory.getSpaceId())
                        .subSpaceId(templateRef.get(task.getTemplateId()))
                        .name(task.getName())
                        .description(task.getDescription())
                        .category(task.getCategory())
                        .type(task.getType())
                        .tags(task.getTags())
                        .duration(task.getDuration())
                        .priority(task.getPriority())
                        .storyPoint(task.getStoryPoint())
                        .createdBy(userId)
                        .createdAt(today)
                        .position(task.getPosition())
                        .assignedUsers(Objects.nonNull(spaceCategory.getAssignedUsers().get(task.getId())) ? Arrays.asList(spaceCategory.getAssignedUsers().get(task.getId())) : null)
                        .status(stage)
                        .build());
            }
            if(!drafts.isEmpty()){
                draftRepository.saveAll(drafts);
            }
            spaceDao.addCategoryAndAddSegmentsToFolder(spaceCategory.getSpaceId(), spaceCategory.getCategory(), folderId, templateRef.keySet().stream().map(key-> templateRef.get(key)).collect(Collectors.toList()));
            auditLogService.save("Initialized "+spaceCategory.getCategory()+" Category", CollectionName.spaces, spaceCategory.getSpaceId(), spaceCategory.getSpaceId(), null);
            return new ResponseEntity<>("Category added", OK);
        }

        return new ResponseEntity<>("Invalid Params", BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> fileUpload(String id, String type, MultipartFile file) {
        return filesService.uploadSpaceDetails(id, type, file);
    }

    @Override
    public ResponseEntity<?> fileList(String id) {
        auditLogService.save("Viewed Client Attachments", CollectionName.spaces, id, id, null);
        return new ResponseEntity<>(spaceDao.getFileList(id), OK);
    }

    @Override
    public ResponseEntity<?> fileDelete(String id) {

        return filesService.deleteSpaceDetails(id);
    }

    @Override
    public void fileDownload(String id, HttpServletResponse response) {
        filesService.downloadSpaceDetails(id, response);
    }

    @Override
    public ResponseEntity<?> updateType(String spaceId, String type) {
        spaceDao.updateType(spaceId, type);
        auditLogService.save("Updated client type", CollectionName.spaces, spaceId, spaceId, null);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<?> findListOfUserFullNames(String id) {
        return new ResponseEntity<>(spaceDao.findListOfUserFullNames(id),OK);
    }

    @Override
    public ResponseEntity<?> inviteBulk(String space, List<String> userIds) {
        List<UsersPermission> usersPermissions = usersPermissionRepository.findAllBySpaceId(space);
        List<UsersPermission> deleteList = new ArrayList<>();
        for(UsersPermission usersPermission : usersPermissions){
            if(!userIds.contains(usersPermission.getUserId())){
                deleteList.add(usersPermission);
            } else {
                userIds.remove(usersPermission.getUserId());
            }
        }
        usersPermissionRepository.deleteAll(deleteList);
        if(!userIds.isEmpty()){
            List<User> users = userService.findAllById(userIds);
            String createdBy = userService.getCurrentUserId();
            Date createdAt = new Date();
            List<UsersPermission> needToSave = new ArrayList<>();
            for(User user : users){
                needToSave.add(UsersPermission
                        .builder()
                        .userId(user.getId())
                        .spaceId(space)
                        .loginId(user.getLoginId())
                        .permissionFor(SPACE)
                        .createdAt(createdAt)
                        .createdBy(createdBy)
                        .build());
            }
            usersPermissionRepository.saveAll(needToSave);
        }
        return new ResponseEntity<>(OK);
    }


}
