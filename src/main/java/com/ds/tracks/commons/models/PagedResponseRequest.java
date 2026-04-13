package com.ds.tracks.commons.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PagedResponseRequest {
    private Integer page;
    private Integer size;
    private Date startDate;
    private Date endDate;
    private String id;
    private String from;
    private String to;
    private String refId;
    private String spaceId;
    private String subSpaceId;
    private String workspaceId;
    private String searchParam;
    private String sortBy;
    private String sortOrder;
    private Boolean app;
    private List<String> tags;
    private List<String> priorities;
    private List<String> severities;
    private List<String> status;
    private List<String> subspaces;
    private List<String> spaces;
    private List<String> categories;
    private List<String> users;
}
