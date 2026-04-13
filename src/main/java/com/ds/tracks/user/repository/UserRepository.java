package com.ds.tracks.user.repository;

import com.ds.tracks.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Arrays;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {


    @Query(value = "{ 'loginId': ?0 }", fields = "{ '_id': 1 }")
    User findIdByLoginId(String name);

    @Query(value = "{ 'loginId': ?0 }", fields = "{ '_id': 1, 'fullName': 1 }")
    User findIdAndFullNameByLoginId(String name);

    User findFirstByLoginIdAndStatus(String userName, String inactive);

    User findFirstByLoginIdAndStatusIn(String username, List<String> status);

    Boolean existsByLoginIdAndStatusIn(String username, List<String> asList);

    @Query(value = "{ 'loginId': ?0 }", fields = "{ 'fullName': 1, 'image':1}")
    User findFullNameAndImageByLoginId(String loginId);

    User findFirstByLoginId(String userName);

    @Query(value = "{ 'loginId': ?0 }", fields = "{ 'password':1 }")
    User findPasswordByLoginId(String loginId);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'fullName':1, 'designation': 1 }")
    User findFirstFullNameAndDesignationById(String signedBy);

    @Query(value = "{ }", fields = "{ 'fullName':1, 'loginId': 1, 'image': 1, 'id': 1 }")
    List<User> findAllFullNameAndEmailAndImage();

    boolean existsByLoginIdAndRole(String name, String admin);

    boolean existsByLoginIdAndAccessIn(String name, List<String> accessPoints);

    @Query(value = "{ 'id': ?0 }", fields = "{ 'fullName':1 }")
    User findFullNameById(String userId);

    @Query(value = "{ 'id': {$in: ?0}  }", fields = "{ 'loginId':1, 'id':1 }")
    List<User> findAllLoginIdById(List<String> userIds);
}
