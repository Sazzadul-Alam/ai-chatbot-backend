package com.ds.tracks.space.model;

import com.ds.tracks.commons.models.enums.ActiveStatus;
import com.ds.tracks.commons.models.UserPermissionGroup;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@Builder
@Document(collection = "spaces")
@NoArgsConstructor
@AllArgsConstructor
public class Space {
    @Id
    private String id;
    private String workspaceId;
    private String address;
    private HashSet<String> tags;
    private List<String> menus;
    private String name;
    private String phone;
    private String image;
    private String mnemonic;
    private String color;
    private String description;
    private Date plannedStartDate;
    private Date plannedEndDate;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
    private String clientType;
    private List<String> categories;
    private SpaceConfigurations configurations;
    private ActiveStatus status = ActiveStatus.ACTIVE;

}
