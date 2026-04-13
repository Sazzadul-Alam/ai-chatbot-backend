package com.ds.tracks.workspace;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    @PostMapping("/create")
    public ResponseEntity<?> createWorkspaces(@RequestBody WorkspaceDto workspace){
        return this.workspaceService.create(workspace);
    }
    @PostMapping("/delete")
    public ResponseEntity<?> deleteWorkspace(@RequestParam String id){
        return this.workspaceService.deleteWorkspace(id);
    }
    @GetMapping("/primary")
    public ResponseEntity<?> getPrimary(){
        return this.workspaceService.getPrimary();
    }
    @PostMapping("/checkPermission")
    public ResponseEntity<?> checkPermission(@RequestParam String id){
//        return new ResponseEntity<>(new Document("canEdit", workspaceService.checkPermission(id)), HttpStatus.OK);
        return null;
    }
    @PostMapping("/initial")
    public ResponseEntity<?> workspaceInitial(@RequestParam(required = false) String id){
        return this.workspaceService.getInitial(id);
    }
    @PostMapping("/available")
    public ResponseEntity<?> availableWorkspaces(@RequestParam("id") String currentWorkspace){
        return workspaceService.availableWorkspaces(currentWorkspace);
    }
    @PostMapping("/update")
    public ResponseEntity<?> updateWorkspace(@RequestParam String id,
                                             @RequestParam String name,
                                             @RequestParam String color,
                                             @RequestParam(required = false) String mnemonic,
                                             @RequestParam(required = false) String vatRegNo,
                                             @RequestParam(required = false) String bankAcc,
                                             @RequestParam(required = false) String bankAccName,
                                             @RequestParam(required = false) String bkashAcc,
                                             @RequestParam(required = false) String corpAddr,
                                             @RequestParam(required = false) Double vatPercentage,
                                             @RequestParam(required = false) String bankName,
                                             @RequestParam(required = false) String bankBranch,
                                             @RequestParam(required = false) String bankNameUAE,
                                             @RequestParam(required = false) String bankAccUAE,
                                             @RequestParam(required = false) String bankAccNameUAE,
                                             @RequestParam(required = false) String bankBranchUAE,
                                             @RequestParam(required = false) String bankSwiftCodeUAE,
                                             @RequestParam(required = false) String bankIbanUAE,
                                             @RequestParam Boolean clearImage,
                                             @RequestParam(required = false) MultipartFile image){
        return workspaceService.update(id, name, corpAddr, mnemonic, color, vatRegNo,bankAcc, bankName, bkashAcc, bankBranch, vatPercentage, clearImage, image, bankAccName, bankNameUAE, bankAccUAE, bankAccNameUAE, bankBranchUAE, bankSwiftCodeUAE, bankIbanUAE);
    }

    @PostMapping("/configurations")
    public ResponseEntity<?> getWorkspaceDetailsForConfig(@RequestParam String id){
        return workspaceService.getConfigurations(id);
    }

    @PostMapping("/{id}/update-stage")
    public ResponseEntity<?> updateStages(@PathVariable String id, @RequestBody List<String> stages){
        return workspaceService.updateStages(id, stages);
    }
    @PostMapping("/transfer-stage")
    public ResponseEntity<?> transferStage(@RequestParam String id,@RequestParam String oldStage, @RequestParam String newStage){
        return workspaceService.transferStage(id, oldStage, newStage);
    }

    @PostMapping("/designation/save")
    public ResponseEntity<?> addDesignation(@RequestParam String id, @RequestParam String designation){
        return workspaceService.addDesignation(id, designation);
    }
    @PostMapping("/designation/edit")
    public ResponseEntity<?> editDesignation(@RequestParam String id, @RequestParam String oldDesignation, @RequestParam String newDesignation){
        return workspaceService.editDesignation(id, oldDesignation, newDesignation);
    }
    @PostMapping("/designation/remove")
    public ResponseEntity<?> removeDesignation(@RequestParam String id, @RequestParam String designation){
        return workspaceService.removeDesignation(id, designation);
    }
    @PostMapping("/invite")
    public ResponseEntity<?> invite(@RequestParam(required = false) String workspace, @RequestParam String email){
        return workspaceService.invite(workspace, email);
    }
    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestParam String workspace, @RequestParam String id){
        return workspaceService.revoke(workspace, id);
    }

    @PostMapping("/project-list")
    public ResponseEntity<?> projectList(@RequestParam String id){
        return workspaceService.projectList(id);
    }
    @PostMapping("/user-list")
    public ResponseEntity<?> userList(@RequestParam String id){
        return workspaceService.userList(id);
    }
    @PostMapping("/allUsers")
    public ResponseEntity<?> allUsers(@RequestParam String id){
        return workspaceService.allUsers(id);
    }
    @PostMapping("/designations")
    public ResponseEntity<?> designations(@RequestParam String id){
        return workspaceService.designations(id);
    }

    @PostMapping("/holiday-list")
    public ResponseEntity<?> holidayList(@RequestParam String id){
        return workspaceService.holidayList(id);
    }

    @PostMapping("/work-configs")
    public ResponseEntity<?> getWorkConfigs(@RequestParam String id){
        return workspaceService.getWorkConfigs(id);
    }

    @PostMapping("/vat-percentage")
    public ResponseEntity<?> vatPercentage(@RequestParam String id){
        return workspaceService.vatPercentage(id);
    }

    @PostMapping("/task-config")
    public ResponseEntity<?> taskConfig(@RequestParam String id){
        return workspaceService.taskConfig(id);
    }

    @PostMapping("/add/{type}")
    public ResponseEntity<?> addTaskConfig(@PathVariable String type, @RequestParam String value, @RequestParam String id){
        return workspaceService.addTaskConfig(type, value, id);
    }

    @PostMapping("/remove/{type}")
    public ResponseEntity<?> removeTaskConfig(@PathVariable String type, @RequestParam String value, @RequestParam String id){
        return workspaceService.removeTaskConfig(type, value, id);
    }


}
