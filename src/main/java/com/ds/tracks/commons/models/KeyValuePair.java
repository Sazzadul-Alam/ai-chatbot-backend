package com.ds.tracks.commons.models;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class KeyValuePair {
    private String key;
    private Object value;

    public KeyValuePair() {
    }

    public KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
