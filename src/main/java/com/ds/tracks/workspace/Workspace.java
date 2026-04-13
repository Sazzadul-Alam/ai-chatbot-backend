package com.ds.tracks.workspace;

import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.enums.ActiveStatus;
import com.ds.tracks.space.model.SpaceConfigurations;
import com.ds.tracks.tasks.model.dto.TaskStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "workspaces")
public class Workspace {
    @Id
    private String id;
    private String name;
    private String corpAddr;
    private String color;
    private String image;
    private String vatRegNo;
    private Integer lastInvoiceNumber;
    private Double vatPercentage;
    private String bankName;
    private String bankAcc;
    private String bkashAcc;
    private String bankBranch;
    private String bankAccName;

    private String bankNameUAE;
    private String bankAccUAE;
    private String bankAccNameUAE;
    private String bankBranchUAE;
    private String bankSwiftCodeUAE;
    private String bankIbanUAE;

    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
    private List<String> designations;
    private SpaceConfigurations configurations;
    private List<String> tags;
    private List<String> categories;
    private List<String> types;
    private List<String> stages;
    private ActiveStatus status = ActiveStatus.ACTIVE;
}
