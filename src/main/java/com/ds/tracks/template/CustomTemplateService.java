package com.ds.tracks.template;

import com.ds.tracks.commons.models.PagedResponseRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CustomTemplateService {
    ResponseEntity<?> getList(PagedResponseRequest id);
    ResponseEntity<?> getTemplate(String id);
    ResponseEntity<?> saveTemplate(CustomTemplate template);
    ResponseEntity<?> deleteTemplate(String id);
    ResponseEntity<?> getTemplateTask(String id);
    ResponseEntity<?> saveTemplateTask(CustomTemplateTasks customTemplate);
    ResponseEntity<?> deleteTemplateTask(String id);
    ResponseEntity<?> clone(List<String> ids, String spaceId, String subspaceId);
    ResponseEntity<?> updateTaskSerial( List<CustomTemplateTasks> tasks);

    ResponseEntity<?> findAllTemplatesByCategory(String category);
}
