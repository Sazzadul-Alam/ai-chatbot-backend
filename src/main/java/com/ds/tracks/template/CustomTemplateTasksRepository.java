package com.ds.tracks.template;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTemplateTasksRepository extends MongoRepository<CustomTemplateTasks, String> {
    @Query(value="{ 'templateId': ?0 }", fields="{ 'name' : 1, 'id':1, 'position': 1 }", sort = "{ 'position': 1 }")
    List<CustomTemplateTasks> findAllNameByTemplateId(String id);
    List<CustomTemplateTasks> findAllByTemplateIdIn(List<String> id);
    List<CustomTemplateTasks> findAllByTemplateId(String id);

    List<CustomTemplateTasks> findAllByTemplateIdInOrderByPosition(List<String> ids);
}
