package com.ds.tracks.user.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user")
public class User implements Serializable {
    private static final long serialVersionUID = -5616176897013108345L;
    @Id
    private String id;
    private String fullName;
    private String loginId;
    private String image;
    private String password;
    private String designation;
    private String otp;
    private Date otpValidationTime;
    private String status;
    private String role;
    private List<String> access;
    private Date createdDate;
    private String createdBy;
    private String phoneNumber;

}
