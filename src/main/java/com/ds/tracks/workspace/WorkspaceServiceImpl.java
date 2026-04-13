package com.ds.tracks.workspace;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.enums.ManagementRoles;
import com.ds.tracks.commons.models.enums.PermissionLayer;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.space.repository.SpaceRepository;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.UsersPermission;
import com.ds.tracks.user.repository.UsersPermissionRepository;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.models.enums.PermissionLayer.SPACE;
import static com.ds.tracks.commons.models.enums.PermissionLayer.WORKSPACE;
import static com.ds.tracks.commons.utils.Utils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;
    private final SpaceRepository spaceRepository;
    private final SpaceDao spaceDao;
    private final TasksDao tasksDao;
    private final AuditLogService auditLogService;
    private final UsersPermissionRepository usersPermissionRepository;

    @Override
    public ResponseEntity<?> create(WorkspaceDto workspace) {
        if (isValidString(workspace.getImage())) {
            String img[] = workspace.getImage().split("base64,");
            if(img.length > 1){
                workspace.setImage(img[1].endsWith(")")? img[1].substring(0, img[1].length() - 1): img[1]);
            }
        } else {
            workspace.setImage(null);
        }
        String currentUser = userService.getCurrentUserId();
        String id = workspaceRepository.save(
                Workspace.builder()
                        .name(workspace.getName())
                        .color(workspace.getColor())
                        .image(workspace.getImage())
                        .designations(workspace.getDesignations())
                        .createdBy(currentUser)
                        .createdAt(new Date())
                        .build()
        ).getId();
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if(Objects.isNull(workspace.getEmails())){
            workspace.setEmails(Arrays.asList(currentUserEmail));
        } else if(!workspace.getEmails().contains(currentUserEmail)) {
            workspace.getEmails().add(currentUserEmail);
        }
        List<User> userList = userService.getAllUserIdByEmail(workspace.getEmails());
        List<UsersPermission> permissions = new ArrayList<>();
        for(User user : userList){
            permissions.add(UsersPermission.builder()
                .workspaceId(id)
                .userId(user.getId())
                .permissionFor(PermissionLayer.WORKSPACE)
                .createdAt(new Date())
                .createdBy(currentUser)
                .role(ManagementRoles.ADMIN)
                .loginId(user.getLoginId())
            .build());
        }
        usersPermissionRepository.saveAll(permissions);
        return ResponseEntity.ok("Workspace Created");
    }
    @Override
    public ResponseEntity<?> update(String id, String name, String corpAddr, String mnemonic, String color, String vatRegNo, String bankAcc, String bankName,
                                    String bkashAcc, String bankBranch, Double vatPercentage, Boolean clearImage, MultipartFile image,
                                    String bankAccName, String bankNameUAE, String bankAccUAE, String bankAccNameUAE, String bankBranchUAE,
                                    String bankSwiftCodeUAE, String bankIbanUAE) {
        if(userService.hasAccess(Arrays.asList(AccessPoints.WORKSPACE_MANAGEMENT))){
            if (isValidString(id) && isValidString(name)) {
                Workspace original = workspaceRepository.findFirstById(id);
                if (Objects.nonNull(original)) {
                    original.setColor(color);
                    original.setName(name);
                    original.setVatRegNo(vatRegNo);
                    original.setCorpAddr(corpAddr);
                    original.setVatPercentage(vatPercentage);

                    // BD
                    original.setBankAcc(bankAcc);
                    original.setBankAccName(bankAccName);
                    original.setBkashAcc(bkashAcc);
                    original.setBankName(bankName);
                    original.setBankBranch(bankBranch);
                    // UAE
                    original.setBankAccUAE(bankAccUAE);
                    original.setBankAccNameUAE(bankAccNameUAE);
                    original.setBankNameUAE(bankNameUAE);
                    original.setBankBranchUAE(bankBranchUAE);
                    original.setBankSwiftCodeUAE(bankSwiftCodeUAE);
                    original.setBankIbanUAE(bankIbanUAE);

                    original.setUpdatedAt(new Date());
                    original.setUpdatedBy(userService.getCurrentUserId());
                    if (Objects.nonNull(image)) {
                        try {
                            original.setImage(Base64.getEncoder().encodeToString(image.getBytes()));
                        } catch (IOException e) {
                            log.error(e.getMessage(), e.getCause());
                        }
                    } else if (Objects.equals(clearImage, true)) {
                        original.setImage(null);
                    }
                    workspaceRepository.save(original);
                    return new ResponseEntity<>("Updated Workspace", HttpStatus.OK);
                }
            }
            return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
        }
        return unauthorized();
    }
    @Override
    public ResponseEntity<?> deleteWorkspace(String id) {
        if (userService.hasAccess(Arrays.asList(AccessPoints.WORKSPACE_MANAGEMENT))) {
            try {
                List<Space> spaces = spaceRepository.findAllIdByWorkspaceId(id);
                if (Objects.nonNull(spaces)) {
                    List<String> spaceIds = spaces.stream().map(Space::getId).collect(Collectors.toList());
                    if (!spaces.isEmpty()) {
                        spaceDao.deleteSpace(spaceIds);
                    }
                }
                spaceDao.deleteWorkspace(id);
                return new ResponseEntity<>("Deleted workspace", HttpStatus.OK);

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return unauthorized();
    }
    @Override
    public boolean checkPermission(String id) {
//        return userService.isSuperAdmin(id);
        return false;
    }

    @Override
    public ResponseEntity<?> projectList(String id) {
        return new ResponseEntity<>(spaceDao.getProjectsInWorkspace(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> userList(String id) {
        return new ResponseEntity<>(usersPermissionRepository.getUsersInWorkspace(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> holidayList(String id) {
        return new ResponseEntity<>(spaceDao.findWorkspaceHolidays(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getWorkConfigs(String id) {
        return new ResponseEntity<>(workspaceRepository.findFirstConfiguration(id).getConfigurations(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> allUsers(String id) {
        auditLogService.save("Viewed user list");
        return new ResponseEntity<>(userService.getAllUsersForWorkspace(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> designations(String id) {
        Workspace workspace = workspaceRepository.findFirstDesignationsById(id);
        return new ResponseEntity<>(workspace.getDesignations(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> vatPercentage(String id) {
        Workspace workspace = workspaceRepository.findVatPercentageById(id);
        return new ResponseEntity<>(Objects.nonNull(workspace) ? workspace.getVatPercentage() : null, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> taskConfig(String id) {
        Workspace workspace = workspaceRepository.findTaskConfigsById(id);
        Document response = new Document();
        if(Objects.nonNull(workspace)){
            if(Objects.nonNull(workspace.getTypes())){
                response.put("types", workspace.getTypes());
            }
            if(Objects.nonNull(workspace.getStages())){
                response.put("stages", workspace.getStages());
            }
            if(Objects.nonNull(workspace.getCategories())){
                response.put("categories", workspace.getCategories());
            }
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> addTaskConfig(String type, String value, String id) {
        spaceDao.addWorkspaceTaskConfig(type, value, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> removeTaskConfig(String type, String value, String id) {
        spaceDao.removeWorkspaceTaskConfig(type, value, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> updateStages(String id, List<String> stages) {
        spaceDao.updateWorkspaceStages(id, stages);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> transferStage(String workspaceId, String oldStage, String newStage) {
        if(isValidString(oldStage) && isValidString(newStage)){
            tasksDao.changeStageOfAllTasksInWorkspace(workspaceId, oldStage, newStage);
            tasksDao.changeStageOfAllTaskDraftsInWorkspace(workspaceId, oldStage, newStage);
            return new ResponseEntity<>(HttpStatus.OK);

        } else {
            return badRequest();
        }
    }

    @Override
    public ResponseEntity<?> getPrimary() {
        return new ResponseEntity<>(spaceDao.findPrimaryWorkspace(SecurityContextHolder.getContext().getAuthentication().getName()), HttpStatus.OK);
    }


    @Override
    public ResponseEntity<?> getInitial(String id) {
        return new ResponseEntity<>(workspaceRepository.findFirstNameAndImageById("64110de0e1c8e61c0194c4e0"), HttpStatus.OK); // better suits their business
//        List<String> workspaces = spaceDao.getWorkspaceList(SecurityContextHolder.getContext().getAuthentication().getName());
//        workspaces.removeAll(Collections.singleton(null));
//        if (workspaces.isEmpty()) {
//            return new ResponseEntity<>(null, HttpStatus.OK);
//        } else {
//            if(Objects.isNull(id)){
//                id = workspaces.get(0);
//            } else if(!workspaces.contains(id)){
//                return new ResponseEntity<>("Workspace not assigned", HttpStatus.UNAUTHORIZED);
//            }
//            return new ResponseEntity<>(workspaceRepository.findFirstById(id), HttpStatus.OK);
//        }
    }

    @Override
    public ResponseEntity<?> availableWorkspaces(String currentWorkspace) {
        return new ResponseEntity<>(spaceDao.availableWorkspaces(currentWorkspace, userService.getCurrentUserId()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getConfigurations(String id) {

        if (isValidString(id)) {
            return new ResponseEntity<>(spaceDao.getWorkspaceDetailsForConfig(id), HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }


    // Designations
    @Override
    public ResponseEntity<?> addDesignation(String id, String designation) {
        designation = designation.trim();
        Workspace workspace = workspaceRepository.findFirstDesignationsById(id);
        boolean exists = false;
        if(Objects.nonNull(workspace.getId())){
            if(Objects.nonNull(workspace.getDesignations())){
                for(String savedDesignation : workspace.getDesignations()){
                    if(Objects.equals(savedDesignation, designation)){
                        exists = true;
                        break;
                    }
                }
            }
            if(exists){
                return new ResponseEntity<>("Already Exists", HttpStatus.OK);
            }
            spaceDao.addDesignation(id, designation);
            return new ResponseEntity<>("Saved", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Workspace not found", HttpStatus.BAD_REQUEST);
        }
    }
    @Override
    public ResponseEntity<?> editDesignation(String workspaceId, String designation, String newDesignation) {
        Workspace workspace = workspaceRepository.findFirstById(workspaceId);
        if(Objects.nonNull(workspace) && Objects.nonNull(workspace.getDesignations())){
            if( workspace.getDesignations().contains(newDesignation)){
                return new ResponseEntity<>("Exists", HttpStatus.OK);
            }
            workspace.getDesignations().remove(designation);
            workspace.getDesignations().add(newDesignation);
            workspaceRepository.save(workspace);
            spaceDao.editDesignation(workspaceId, designation, newDesignation);
            return new ResponseEntity<>("Saved",HttpStatus.OK);
        }
        return badRequest();
    }
    @Override
    public ResponseEntity<?> removeDesignation(String workspaceId, String designation) {
        spaceDao.removeDesignation(workspaceId, designation);
        return new ResponseEntity<>("Removed", HttpStatus.OK);
    }

    // Superuser
    @Override
    public ResponseEntity<?> invite(String workspaceId, String email) {
        if(!isValidString(workspaceId)){
            if(!isValidString(email)){
                return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
            }
            IdNameRelationDto user = userService.getUserIdAndName(email);
            if(Objects.isNull(user)){
                return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(new Document("name", user.getName()).append("id", user.getId()).append("email", email), HttpStatus.OK);
        }else {
            if(!isValidString(email)){
                return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
            }
            IdNameRelationDto user = userService.getUserIdAndName(email);
            if(Objects.isNull(user)){
                return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
            }
            if(usersPermissionRepository.existsByWorkspaceIdAndLoginIdAndPermissionFor(workspaceId, email, WORKSPACE)) {
                return new ResponseEntity<>("User already Added", HttpStatus.BAD_REQUEST);
            }
            String createdBy = userService.getCurrentUserId();
            UsersPermission usersPermission = usersPermissionRepository.save(UsersPermission.builder()
                    .createdAt(new Date())
                    .createdBy(createdBy)
                    .workspaceId(workspaceId)
                    .loginId(email.trim())
                    .userId(user.getId().toString())
                    .permissionFor(WORKSPACE)
                    .role(ManagementRoles.ADMIN).build());
            addToAllExistingProjects(usersPermission.getWorkspaceId(), usersPermission.getUserId(), usersPermission.getLoginId(), usersPermission.getCreatedBy());
            return new ResponseEntity<>(new Document("name", user.getName()).append("id", user.getId()).append("email", email), HttpStatus.OK);
        }

    }
    @Override
    public ResponseEntity<?> revoke(String workspaceId, String userId) {
        spaceDao.removeWorkspaceSuperuser(workspaceId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    private void addToAllExistingProjects(String workspaceId, String userId, String loginId, String currentUser) {
        List<UsersPermission> usersPermissions = new ArrayList<>();
        List<String> needToAddTo = spaceDao.getAllProjectsUserNotAddedIn(userId, workspaceId);
        if (Objects.nonNull(needToAddTo)) {
            for (String spaceId : needToAddTo) {
                usersPermissions.add(UsersPermission.builder()
                        .createdAt(new Date())
                        .createdBy(currentUser)
                        .spaceId(spaceId)
                        .workspaceId(workspaceId)
                        .loginId(loginId)
                        .userId(userId)
                        .permissionFor(SPACE)
                        .role(ManagementRoles.OBSERVER).build());
            }
            usersPermissionRepository.saveAll(usersPermissions);
        }
    }
}