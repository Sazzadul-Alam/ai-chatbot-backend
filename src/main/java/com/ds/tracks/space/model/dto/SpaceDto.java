package com.ds.tracks.space.model.dto;

import com.ds.tracks.tasks.model.dto.TaskStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Base64;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SpaceDto {
    private String workspaceId;
    private String id;
    private String name;
    private String phone;
    private String address;
    private String description;
    private Date startDate;
    private Date endDate;
    private String color;
    private String mnemonic;
    private List<String> emails;
    private List<String> types;
    private List<String> removeTypes;
    private List<String> removeCategories;
    private List<TaskStatus> status;
    private List<String> category;
    private List<String> menus;
    private String source;
    private String image;
    private String clientType;
    private List<String> clientCategories;
    private String lockStage;

}
