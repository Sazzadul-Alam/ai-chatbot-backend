package com.ds.tracks.commons.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Response implements Serializable {
    private static final long serialVersionUID = -5616176897013108345L;
    private Integer statusCode;
    private String desc;

    public Response(Integer statusCode, String desc) {
        this.statusCode = statusCode;
        this.desc = desc;
    }
}


