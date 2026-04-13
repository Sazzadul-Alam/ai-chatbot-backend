package com.ds.tracks.user.service;

import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.dto.UserInfoDto;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface UserService {
    List<String> getAllUserNameAsList();
    Map<String, String> getUserNameAndPassword();
    Boolean getActiveStatusByUserName(String username);
    User getByUserName(String userName);
    ResponseEntity<?> update(User user);
    ResponseEntity<?> activate(String loginId, String otp);
    List<User> findByLogidIdAndStatus(String loginId,String status);
    User findUserByLoginId(String username);
    Map<String, String> findActiveUsersByLoginId(String username);
    ResponseEntity<?> findActiveUsers();
    ResponseEntity<?> otpReset(String loginId);

    String getCurrentUserId();
    IdNameRelationDto getCurrentUserFullName();
    IdNameRelationDto getUserIdAndName(String email);

    ResponseEntity<?> register(String name, String email, String password,String phoneNumber);

    List<User> findAllById(List<String> userIds);
    Map<String, String> findUsersByLoginIds(List<String> username);



    User findFirstByLoginIdAndStatusIn(String username, List<String> status);

    void updateStatus(String active, String userId);

    UserInfoDto getUserInfo();

    ResponseEntity<?> update(MultipartFile image, String name);

    ResponseEntity<?> verifyPassword(String password);

    ResponseEntity<?> changePassword(String password, Boolean resetTokens);

    List<Document> getUsersByIds(List<String> assignedUsers);

    List<User> getAllUserIdByEmail(List<String> email);

    boolean hasAccess(List<String> accessPointName);
    boolean hasAccessOrIsClientAdmin(List<String> accessPointName, String spaceId);

    User getUserNameAndDesignationById(String signedBy);

    Object getAllUsersForWorkspace(String id);

    ResponseEntity<?> updateUser(User user, HttpServletRequest request);

    ResponseEntity<?> createUser(UserInfoDto user, HttpServletRequest request);

    ResponseEntity<?> resetPasswordRequestOTP(String email);

    ResponseEntity<?> resetPasswordVerifyOTP(String email, String otp);

    ResponseEntity<?> resetPasswordUpdatePassword(String email, String password, String token);

    ResponseEntity<?> resetPassword(String email, String workspaceId);

    List<String> getUserAccess();

    Object getAllUsersWithNames();

    ResponseEntity<?> findAllActiveUsers();

    String getUsernameById(String userId);

    Boolean checkIfCurrentUserIsSpecialUser();
}
