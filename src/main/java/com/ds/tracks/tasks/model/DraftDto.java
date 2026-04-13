package com.ds.tracks.tasks.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import java.util.List;

@Getter
@Setter
public class DraftDto extends TaskDraft{
    private List<Document> assigned;
    private boolean canEdit;
}
