package com.ds.tracks.commons.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InactiveUserLogin implements Serializable {
    private static final long serialVersionUID = -5616176897013108345L;
    private Integer statusCode;
    private String userId;
    private String desc;
    private Boolean isFirstLogin;

}
