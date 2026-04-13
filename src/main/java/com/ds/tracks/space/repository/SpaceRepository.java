package com.ds.tracks.space.repository;

import com.ds.tracks.space.model.Space;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public interface SpaceRepository extends MongoRepository<Space, String> {

    Space findFirstById(String id);

    @Query(value = "{ 'workspaceId': ?0 }", fields = "{ 'name':1, 'configurations.color':1 }")
    List<Map<String, Objects>> findAllByWorkspaceId(String workspaceId);
    @Query(value = "{ 'workspaceId': ?0 }", fields = "{ 'id':1 }")
    List<Space> findAllIdByWorkspaceId(String workspaceId);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'mnemonic':1, 'id':0 }")
    Space getMnemonicById(String spaceId);

//    @Query(value = "{ 'id': ?0 }", fields = "{ 'configurations':1, 'id':0 }")
//    List<Space> findConfigurationsById(String spaceId);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'configurations.holiday':1, 'id':0 }")
    List<Space> findHolidayConfigurationsById(String spaceId);

    @Query(value = "{ 'id': ?0 }", fields = "{  'mnemonic':1,'configurations':1, 'id':0, 'workspaceId': 1 }")
    Space findFirstConfigurationsAndMnemonicById(String spaceId);

    @Query(value = "{ 'id': ?0 }", fields = "{  'configurations.workHour':1, 'id':0 }")
    Space findWorkHourById(String spaceId);


    @Query(value = "{ 'id': ?0 }", fields = "{ 'configurations':1, 'id':0 }")
    Optional<Space> findConfigurationsById(String spaceId);

    @Query(value = "{ 'workspaceId': ?0 }", fields = "{ 'name':1, 'color':1, 'id':1, 'clientType': 1 }")
    List<Space> findAllNameAndColorByWorkspaceId(String workspaceId);

    @Query(value = "{ 'workspaceId': ?0, 'id': { $in: ?1 } }", fields = "{ 'name':1, 'color':1, 'id':1, 'clientType': 1 }")
    List<Space> findAllNameAndColorByWorkspaceIdAndIdIn(String workspaceId, List<String> spaces);
    @Query(value = "{ 'id': ?0 }", fields = "{ 'image':1 }")

    Space findFirstImageById(String id);

    @Query(value = "{  }", fields = "{ 'id':1 }")
    List<Space> findAllId();
}
