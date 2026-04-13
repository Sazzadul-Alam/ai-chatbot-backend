package com.ds.tracks.user.model.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDto {
    private String id;
    private String name;
    private String email;
    private String image;
    private String designation;
    private String role;
}
