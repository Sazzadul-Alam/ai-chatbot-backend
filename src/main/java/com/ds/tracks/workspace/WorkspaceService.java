package com.ds.tracks.workspace;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WorkspaceService {
    ResponseEntity<?> create(WorkspaceDto workspace);
    ResponseEntity<?> update(String id, String name, String corpAddr, String mnemonic, String color, String vatRegNo, String bankAcc, String bankName, String bkashAcc, String bankBranch, Double vatPercentage, Boolean clearImage, MultipartFile image, String bankAccName, String bankNameUAE, String bankAccUAE, String bankAccNameUAE, String bankBranchUAE, String bankSwiftCodeUAE, String bankIbanUAE);

    ResponseEntity<?> deleteWorkspace(String id);


    ResponseEntity<?> getPrimary();
    ResponseEntity<?> getInitial(String id);
    ResponseEntity<?> availableWorkspaces(String currentWorkspace);


    ResponseEntity<?> addDesignation(String id, String designation);
    ResponseEntity<?> editDesignation(String id, String designation, String newDesignation);
    ResponseEntity<?> removeDesignation(String id, String designation);


    ResponseEntity<?> invite(String id, String email);
    ResponseEntity<?> revoke(String workspace, String id);


    ResponseEntity<?> getConfigurations(String id);
    boolean checkPermission(String workspaceId);

    ResponseEntity<?> projectList(String id);

    ResponseEntity<?> userList(String id);

    ResponseEntity<?> holidayList(String id);

    ResponseEntity<?> getWorkConfigs(String id);

    ResponseEntity<?> allUsers(String id);

    ResponseEntity<?> designations(String id);

    ResponseEntity<?> vatPercentage(String id);

    ResponseEntity<?> taskConfig(String id);

    ResponseEntity<?> addTaskConfig(String type, String value, String id);

    ResponseEntity<?> removeTaskConfig(String type, String value, String id);

    ResponseEntity<?> updateStages(String id, List<String> stages);

    ResponseEntity<?> transferStage(String id, String oldStage, String newStage);
}
