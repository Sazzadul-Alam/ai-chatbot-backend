package com.ds.tracks.user.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPermissionDto {
    private String userId;
    private String spaceId;
    private String subSpaceId;
    private Integer capacity;
}
