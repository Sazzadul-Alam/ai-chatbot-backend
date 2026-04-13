package com.ds.tracks.template;

import com.ds.tracks.commons.models.PagedResponseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/template")
@RequiredArgsConstructor
public class CustomTemplateController {

    private final CustomTemplateService templatesService;

    @PostMapping("/list")
    public ResponseEntity<?> getList(@RequestBody PagedResponseRequest request) {
        return templatesService.getList(request);
    }
    @PostMapping("/get")
    public ResponseEntity<?> getTemplate(@RequestParam String id) {
        return templatesService.getTemplate(id);
    }
    @PostMapping("/save")
    public ResponseEntity<?> saveTemplate(@RequestBody CustomTemplate template){
        return templatesService.saveTemplate(template);
    }
    @PostMapping("/delete")
    public ResponseEntity<?> deleteTemplate(@RequestParam String id){
        return templatesService.deleteTemplate(id);
    }

    @PostMapping("/get-task")
    public ResponseEntity<?> getTemplateTask(@RequestParam String id){
        return templatesService.getTemplateTask(id);
    }
    @PostMapping("/save-task")
    public ResponseEntity<?> saveTemplateTask(@RequestBody CustomTemplateTasks customTemplate){
        return templatesService.saveTemplateTask(customTemplate);
    }
    @PostMapping("/delete-task")
    public ResponseEntity<?> deleteTemplateTask(@RequestParam String id){
        return templatesService.deleteTemplateTask(id);
    }

    @PostMapping("/clone/{spaceId}/{subspaceId}")
    public ResponseEntity<?> clone(@PathVariable String spaceId, @PathVariable String subspaceId, @RequestBody List<String> ids) {
        return templatesService.clone(ids, spaceId, subspaceId);
    }

    @PostMapping("/update-position")
    public ResponseEntity<?> updateTaskSerial(@RequestBody List<CustomTemplateTasks> tasks) {
        return templatesService.updateTaskSerial(tasks);
    }

    @PostMapping("/findAllTemplatesByCategory")
    public ResponseEntity<?> findAllTemplatesByCategory(@RequestParam String category) {
        return templatesService.findAllTemplatesByCategory(category);
    }
}
