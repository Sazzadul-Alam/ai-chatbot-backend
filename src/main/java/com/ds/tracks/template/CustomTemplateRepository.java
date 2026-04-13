package com.ds.tracks.template;

import com.ds.tracks.template.CustomTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTemplateRepository extends MongoRepository<CustomTemplate, String> {

    List<CustomTemplate> findAllByIdIn(List<String> ids);

    @Query(value="{  }", fields="{ 'name' : 1, 'category':1, 'id':1 }")
    List<CustomTemplate> findAllBasicInfo();

    List<CustomTemplate> findAllByCategoryIn(List<String> categories);
    @Query(value="{ }", fields="{ 'id' : 1, 'name': 1 }")
    List<CustomTemplate> findAllId();
}
