package com.ds.tracks.template;

import java.util.List;
import java.util.Map;

public interface TemplateDao {

    List<Map<String, Object>> findTemplatesByCategory(String category);

    List<Map<String, Object>>  findTemplatesByTasks(List<String> selectedTasks);

    void updateTaskSerial(List<CustomTemplateTasks> tasks);
}
