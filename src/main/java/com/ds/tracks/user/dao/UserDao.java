package com.ds.tracks.user.dao;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.user.model.User;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface UserDao {
    User save(User user);
    UpdateResult update(User user);
    UpdateResult activate(String loginId, String otp);
    UpdateResult invitedPrivilegeCreate(String loginId,String userId);
    int deleteById(String id);
    User findById(String id);
    User findByLoginId(String loginId);
    List<UserDao> findAll(Integer page, Integer size);
    List<User> findByLogidIdAndStatus(String loginId,String status);
    User findUserByLoginId(String username);
    Map<String, String> findActiveUsersByLoginId(String username);
    Map<String, String> findActiveUsers();
    UpdateResult otpReset(String loginId, String otp, Date otpValidationTime);
    Map<String, String> findUsersByLoginIds(List<String> username);

    void updateStatus(String userId, String newStatus);

    UpdateResult update(String imageString, String name);

    void updatePassword(String name, String password);

    List<Document> getUsersByIds(List<String> userIds);

    Boolean hasPermission(String spaceId, String loginId);

    boolean hasWSPermission(String workspaceId, String name);

    List<User> getUsersByEmails(List<String> email);

    List<Document> findAllUserIdAndNameAndImage();

    Object getAllUsersForWorkspace(String id);

    void updateUser(User user);

    List<String> getAccess(String loginId);

    Object getAllUsersWithNames();
    //V2
    Object findAllActiveUsers();
}
