package com.ds.tracks.user.serviceImpl;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.exception.UserNotFoundException;
import com.ds.tracks.commons.models.IdNameRelationDto;
import com.ds.tracks.commons.models.enums.ManagementRoles;
import com.ds.tracks.commons.models.enums.PermissionLayer;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.Utils;
import com.ds.tracks.commons.utils.UserStatus;
import com.ds.tracks.security.repository.TokenStoreRepository;
import com.ds.tracks.user.dao.UserDao;
import com.ds.tracks.user.model.AccessPoints;
import com.ds.tracks.user.model.Otp;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.dto.UserInfoDto;
import com.ds.tracks.user.repository.*;
import com.ds.tracks.commons.response.Response;
import com.ds.tracks.user.service.EmailService;
import com.ds.tracks.user.service.UserService;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.ds.tracks.commons.utils.UserStatus.*;
import static com.ds.tracks.commons.utils.Utils.isValidEmail;
import static com.ds.tracks.commons.utils.Utils.isValidString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UsersPermissionRepository permissionRepository;
    private final TokenStoreRepository tokenStoreRepository;
    private final UserDao userDao;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${otp.generation.time}")
    private Integer otpGenerationTime;
//    @Value("${otp.generation.time}")
    @Value("${app.url}")
    private String link;

    @Override
    public List<String> getAllUserNameAsList() {
        return userRepository.findAll().stream()
                .map(User::getLoginId).collect(toList());
    }

    @Override
    public Map<String, String> getUserNameAndPassword() {
        return userRepository.findAll().stream()
                .collect(toMap(User::getLoginId, User::getPassword, (a, b) -> b));
    }

    @Override
    public Boolean getActiveStatusByUserName(String username) {
        return userRepository.existsByLoginIdAndStatusIn(username, Arrays.asList(ONBOARDING, ACTIVE));
    }

    @Override
    public User getByUserName(String userName) {
//        return userRepository.findFirstByLoginIdAndStatus(userName, NEW);
        return userRepository.findFirstByLoginId(userName);
    }


    @Override
    public ResponseEntity<?> update(User user) {
        try {
            UpdateResult updateResult = userDao.update(user);
            if (updateResult.getModifiedCount() > 0) {
                return new ResponseEntity<>(new Response(200, "User is Updated!"), HttpStatus.OK);
            } else {
                log.info("User is not Updated.");
                return new ResponseEntity<>(new Response(200, "User is not Updated."), HttpStatus.OK);
            }
        } catch (org.springframework.dao.DuplicateKeyException dke) {
            log.error("Error => {} \n Reason => {} \n Strace => {}", dke.getMessage(), dke.getCause(), dke);
            return new ResponseEntity<>(new Response(409, "Login Id is existed"), HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Error => {} \n Reason => {} \n Strace => {}", e.getMessage(), e.getCause(), e);
            return new ResponseEntity<>(new Response(500, "Failed to Update user!"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> activate(String loginId, String otp) {
        try {
            User user = userRepository.findFirstByLoginId(loginId);
            if(Objects.nonNull(user) && Objects.equals(user.getOtp(), otp)){
                if(Objects.nonNull(user.getOtpValidationTime()) &&
                        new Date().compareTo(user.getOtpValidationTime()) < 0){
                    if (userDao.activate(loginId, otp).getModifiedCount() > 0) {
                        userDao.invitedPrivilegeCreate(loginId, user.getId());
                        return new ResponseEntity<>("OTP Verified", HttpStatus.OK);
                    }
                } else {
                    return new ResponseEntity<>("OTP Expired", HttpStatus.BAD_REQUEST);
                }
            }
            return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            return new ResponseEntity<>("Something went wrong",  HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<User> findByLogidIdAndStatus(String loginId, String status) {
        return userDao.findByLogidIdAndStatus(loginId, status);
    }

    @Override
    public User findUserByLoginId(String username) {
        return userDao.findUserByLoginId(username);
    }

    @Override
    public Map<String, String> findActiveUsersByLoginId(String username) {
        return userDao.findActiveUsersByLoginId(username);
    }

    @Override
    public ResponseEntity<?> findActiveUsers() {
        try {
            Map<String, String> activeUsers = userDao.findActiveUsers();
            if (Objects.nonNull(activeUsers)) {
                return new ResponseEntity<>(activeUsers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Response(200, "No data Found."), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Failed to fetch active users!");
            return new ResponseEntity<>(new Response(500, "Failed to fetch active users!"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> otpReset(String loginId) {
        try {
            String otp = Utils.randomIdGenerate();
            Date otpValidationDate = Utils.addHoursToJavaUtilDate(new Date(), otpGenerationTime);
            UpdateResult updateResult = userDao.otpReset(loginId, otp, otpValidationDate);
            if (updateResult.getModifiedCount() > 0) {
                emailService.sendEmail(loginId, "OTP Verification Code – Resent", "Welcome to iSAGE, \n Your OTP is " + otp);
                return new ResponseEntity<>(new Response(200, "New otp is generated. It will be expired within " + otpGenerationTime + " minutes."), HttpStatus.OK);
            } else {
                log.info("New otp is not generated.");
                return new ResponseEntity<>(new Response(200, "New otp is not generated."), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Failed to generate otp");
            return new ResponseEntity<>(new Response(500, "Failed to create user!"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getCurrentUserId() {
        try {
            User user = userRepository.findIdByLoginId(SecurityContextHolder.getContext().getAuthentication().getName());
            if (Objects.nonNull(user)) {
                return user.getId();
            } else {
                throw new UsernameNotFoundException("User not found");
            }
        } catch (Exception e) {
            log.error("User not found");
            return null;
        }
    }

    @Override
    public IdNameRelationDto getCurrentUserFullName() {
        User user = userRepository.findIdAndFullNameByLoginId(SecurityContextHolder.getContext().getAuthentication().getName());
        if (Objects.nonNull(user)) {
            return new IdNameRelationDto(user.getId(), user.getFullName());
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    @Override
    public IdNameRelationDto getUserIdAndName(String email) {
        User user = userRepository.findIdAndFullNameByLoginId(email);
        if (Objects.nonNull(user)) {
            return new IdNameRelationDto(user.getId(), user.getFullName());
        } else {
            return null;
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> register(String name, String email, String password,String phoneNumber) {
        if (isValidString(name) && isValidEmail(email) && isValidString(password)) {
            User user = userRepository.findFirstByLoginId(email);;
            if(Objects.isNull(user) || Objects.equals(user.getStatus(), NEW)){
                return  registerUser(Objects.nonNull(user) ? user.getId() : null, name, email, password,phoneNumber);
            } else {
                return new ResponseEntity<>("Email already exists", HttpStatus.CONFLICT);
            }
        }
        return new ResponseEntity<>("Please provide valid information", HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<?> registerUser(String id, String name, String email, String password,String phoneNumber) {
        try {
            String otp = Utils.randomIdGenerate();
            userDao.save(
                    User.builder()
                            .id(id)
                            .createdDate(new Date())
                            .fullName(name.trim())
                            .loginId(email.trim())
                            .phoneNumber(phoneNumber.trim())
                            .password(new BCryptPasswordEncoder(8).encode(password))
                            .otp(otp)
                            .otpValidationTime(Utils.addHoursToJavaUtilDate(new Date(), otpGenerationTime))
                            .status(UserStatus.NEW)
                            .build());
            emailService.sendEmail(email.trim(), "OTP Verification Code", "Welcome to iSAGE, \n Your OTP is " + otp);
            return new ResponseEntity<>("Otp Sent", HttpStatus.OK);
        } catch (org.springframework.dao.DuplicateKeyException duplicateKeyException) {
            return new ResponseEntity<>("Email already exists", HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<User> findAllById(List<String> userIds) {
        return userRepository.findAllLoginIdById(userIds);
    }

    @Override
    public Map<String, String> findUsersByLoginIds(List<String> username) {
        return userDao.findUsersByLoginIds(username);
    }

    @Override
    public User findFirstByLoginIdAndStatusIn(String username, List<String> status) {
        return userRepository.findFirstByLoginIdAndStatusIn(username, status);
    }

    @Override
    public void updateStatus(String newStatus, String userId) {
        userDao.updateStatus(userId, newStatus);
    }

    @Override
    public UserInfoDto getUserInfo() {
        try{
            final String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findFullNameAndImageByLoginId(loginId);
            if(Objects.nonNull(user)){
                return UserInfoDto.builder().id(user.getId()).name(user.getFullName()).email(loginId).image(user.getImage()).build();
            }
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
        }
        throw new UserNotFoundException("User Not found");
    }

    @Override
    public ResponseEntity<?> update(MultipartFile image, String name) {
        try {
            String imageString = null;
            if(Objects.nonNull(image)){
                imageString = Base64.getEncoder().encodeToString(image.getBytes());

            }
            UpdateResult updateResult = userDao.update(imageString, name);
            return new ResponseEntity<>(updateResult.getModifiedCount() > 0 ? "User Updated" : "User not updated", HttpStatus.OK);
        }catch (Exception e){
            log.error("Error Message => {} \n Error Reason => {} \n Strace => {}", e.getMessage(),e.getCause(),e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<?> verifyPassword(String password) {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findPasswordByLoginId(loginId);
        if(Objects.nonNull(user) && isValidString(user.getPassword())){
            if(BCrypt.checkpw(password, user.getPassword())){
                return new ResponseEntity<>("Password Verified", HttpStatus.OK);
            }
            return new ResponseEntity<>("Invalid Password", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Invalid User", HttpStatus.UNAUTHORIZED);
    }

    @Override
    public ResponseEntity<?> changePassword(String password, Boolean resetTokens) {
        if(isValidString(password)){
            String loginId =SecurityContextHolder.getContext().getAuthentication().getName();
            IdNameRelationDto idNameRelationDto = getCurrentUserFullName();
            password = new BCryptPasswordEncoder(8).encode(password);
            userDao.updatePassword(loginId, password);
            if(Objects.equals(resetTokens, true)){
                tokenStoreRepository.deleteAllByLoginId(loginId);
            }
            auditLogService.save(idNameRelationDto.getName().toString()+" Updated password", CollectionName.user, idNameRelationDto.getId().toString());
            return new ResponseEntity<>("Password Updated", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid Params", HttpStatus.BAD_REQUEST);
    }

    @Override
    public List<Document> getUsersByIds(List<String> userIds) {
        return userDao.getUsersByIds(userIds);
    }


    @Override
    public List<User> getAllUserIdByEmail(List<String> email) {
        return userDao.getUsersByEmails(email);
    }


    @Override
    public boolean hasAccess(List<String> accessPointNames) {
        try{
            return userRepository.existsByLoginIdAndAccessIn(SecurityContextHolder.getContext().getAuthentication().getName(), accessPointNames);
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    @Override
    public boolean hasAccessOrIsClientAdmin(List<String> accessPointNames, String spaceId) {
        boolean access = false;
        try{
            access = userRepository.existsByLoginIdAndAccessIn(SecurityContextHolder.getContext().getAuthentication().getName(), accessPointNames);
        } catch (Exception ignored){}
        if(!access){
            access = permissionRepository.existsBySpaceIdAndLoginIdAndPermissionForAndRoleIn(spaceId,SecurityContextHolder.getContext().getAuthentication().getName(), PermissionLayer.SPACE, Arrays.asList(ManagementRoles.ADMIN) );
        }
        return access;
    }

    @Override
    public User getUserNameAndDesignationById(String signedBy) {
        return  userRepository.findFirstFullNameAndDesignationById(signedBy);
    }

    @Override
    public Object getAllUsersForWorkspace(String id) {
        return userDao.getAllUsersForWorkspace(id);
    }

    @Override
    public ResponseEntity<?> updateUser(User user, HttpServletRequest request) {
        String fullName = userRepository.findFullNameById(user.getId()).getFullName();
        userDao.updateUser(user);
        auditLogService.save( "Updated User -> "+fullName, CollectionName.user, user.getId());
        return new ResponseEntity<>("User updated", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> createUser(UserInfoDto userDto, HttpServletRequest request) {
        if(isValidString(userDto.getName()) && isValidString(userDto.getEmail())){
            if(userRepository.existsByLoginIdAndStatusIn(userDto.getEmail().trim().toLowerCase(), Arrays.asList(ACTIVE, INACTIVE))){
                return new ResponseEntity<>("User already Exists", HttpStatus.BAD_REQUEST);
            }
            String password = generateRandomPassword();
            User user = new User();
            user.setFullName(userDto.getName().trim());
            user.setLoginId(userDto.getEmail().trim().toLowerCase());
            user.setDesignation(userDto.getDesignation());
            user.setStatus(ACTIVE);
            user.setRole(Arrays.asList("USER", "ADMIN").contains(userDto.getRole())? userDto.getRole() : "USER");
            user.setCreatedDate(new Date());
            user.setCreatedBy(getCurrentUserId());
            if(isValidString(userDto.getImage())){
                String image = userDto.getImage().split("base64,")[1];
                if(image.endsWith(")")){
                    image = image.substring(0, image.length() - 1);
                }
                user.setImage(image);
            } else {
                user.setImage(null);
            }
            user.setPassword(new BCryptPasswordEncoder(8).encode(password));
            userRepository.save(user);
            auditLogService.save("Created User -> "+user.getLoginId(), CollectionName.user, user.getId());
            emailService.sendEmail(user.getLoginId(), "User Notification", "Welcome to Accfintax, \n Your Login Id is   " + user.getLoginId() + "\n Your Password is  "+ password + "\n You can login using this link : "+ link);
            return new ResponseEntity<>("User Created", HttpStatus.OK);
        }
        return new ResponseEntity<>("Please Fill All Mandatory Fields", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> resetPasswordRequestOTP(String email) {
        if(userRepository.existsByLoginIdAndStatusIn(email, Arrays.asList(ACTIVE))){
            try{
                Otp otp = otpRepository.findByLoginIdAndReason(email, "FORGET_PASSWORD").orElse(new Otp());
                otp.setLoginId(email);
                otp.setOtp(Utils.randomIdGenerate());
                otp.setReason("FORGET_PASSWORD");
                otpRepository.save(otp);
                emailService.sendEmail(email, "Reset Password",
                        "Dear User,\n\n" +
                        "You have requested to access your account using a one-time password (OTP).\n" +
                        "Your OTP is "+otp.getOtp()+".\n" +
                        "Please enter this code on the website to verify your identity and proceed with your request.\n\n" +
                        "If you did not request an OTP, please ignore this email or contact our support team.\n\n" +
                        "Thank you for using our service.");
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e){
                log.error(e.getMessage(), e.getCause());
            }
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> resetPasswordVerifyOTP(String email, String otp) {
        Otp otpRequest = otpRepository.findByLoginIdAndReasonAndOtp(email, "FORGET_PASSWORD", otp).orElse(null);
        if(Objects.nonNull(otpRequest)){
            otpRequest.setRequestToken(generateRequestToken());
            otpRepository.save(otpRequest);
            return new ResponseEntity<>(otpRequest.getRequestToken(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private String generateRequestToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public ResponseEntity<?> resetPasswordUpdatePassword(String email, String password, String token) {
        try{
            Otp otpRequest = otpRepository.findByLoginIdAndReasonAndRequestToken(email, "FORGET_PASSWORD", token).orElse(null);
            if(Objects.nonNull(otpRequest)){
                User user = userRepository.findFirstByLoginId(email);
                user.setPassword(new BCryptPasswordEncoder(8).encode(password));
                userRepository.save(user);
                otpRepository.delete(otpRequest);
                return new ResponseEntity<>(HttpStatus.OK);
            }
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> resetPassword(String email, String workspaceId) {
        if(hasAccess(Arrays.asList(AccessPoints.ACCOUNT_MANAGEMENT))){
            User user = userRepository.findFirstByLoginId(email);
            if(Objects.nonNull(user)){
                user.setPassword(new BCryptPasswordEncoder(8).encode(generateRandomPassword()));
                userRepository.save(user);
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return Utils.badRequest();
        }

        return Utils.unauthorized();
    }

    @Override
    public List<String> getUserAccess() {
        List<String> access = null;
        try{
            String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
            access = userDao.getAccess(loginId);
        } catch (Exception ignored){}
        return Objects.nonNull(access) ? access : Collections.emptyList();
    }

    @Override
    public Object getAllUsersWithNames() {
        return userDao.getAllUsersWithNames();
    }


    @Override
    public ResponseEntity<?> findAllActiveUsers() {
        return new ResponseEntity<>(userDao.findAllActiveUsers(), HttpStatus.OK);
    }

    @Override
    public String getUsernameById(String userId) {
        return userRepository.findFullNameById(userId).getFullName();
    }

    @Override
    public Boolean checkIfCurrentUserIsSpecialUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        return Arrays.asList("murshed@accfintax-bd.com", "musrufe@accfintax-bd.com", "faisal@accfintax-bd.com").contains(name);
    }


    private String generateRandomPassword() {
        return "Tracks@123";
    }

//    @PostConstruct
//    private void initializeRole(){
//        User user = userRepository.findFirstByLoginId("support@datasoft-bd.com");
//        user.setAccess(Arrays.asList(
//                "Account Management",
//                "Workspace Management",
//                "Client Management",
//                "Task Management",
//                "Audit Log",
//                "Invoice",
//                "Reports",
//                "Workload",
//                "Insights",
//                "Gantt"
//        ));
//        userDao.updateUser(user);
//        log.info("Initial User Set");
//    }



}
