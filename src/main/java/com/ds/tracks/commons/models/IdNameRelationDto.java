package com.ds.tracks.commons.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdNameRelationDto {
    private Object id;
    private Object name;

    public IdNameRelationDto(Object id, Object name) {
        this.id = id;
        this.name = name;
    }

    public IdNameRelationDto() {
    }
}
