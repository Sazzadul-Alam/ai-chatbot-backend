package com.ds.tracks.workspace;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkspaceDto {
    private String name;
    private String color;
    private String image;
    private List<String> emails;
    private List<String> designations;
}
