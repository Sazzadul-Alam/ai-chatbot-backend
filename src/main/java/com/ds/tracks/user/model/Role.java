package com.ds.tracks.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "role")
public class Role implements Serializable {
    private static final long serialVersionUID = -5616176897013108345L;
    private String id;
    private String roleName;
    private List<String> taskIds = new ArrayList<>();
    private String status;
    private Date createdDate;
    private Date updatedDate;
    private String createdBy;
    private String updatedBy;
    private String createdById;
    private String updatedById;
    private String reason;
}
