package com.ds.tracks.user.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document("notes")
public class Notes {
    @Id
    private String userId;
    private String note;

    public Notes() {
    }

    public Notes(String userId, String note) {
        this.userId = userId;
        this.note = note;
    }
}
