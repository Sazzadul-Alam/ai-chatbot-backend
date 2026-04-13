package com.ds.tracks.user.controller;

import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.dto.UserInfoDto;
import com.ds.tracks.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> addUser(@RequestParam String name,
                                     @RequestParam String email,
                                     @RequestParam String phoneNumber,
                                     @RequestParam String password) {
        return userService.register(name, email, password,phoneNumber);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo() {
        return new ResponseEntity<>(userService.getUserInfo(), HttpStatus.OK);
    }

    @GetMapping("/access")
    public ResponseEntity<?> getAccess() {
        return new ResponseEntity<>(userService.getUserAccess(),HttpStatus.OK);
    }
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestParam(required = false) MultipartFile image, @RequestParam String name) {
        return userService.update(image, name);
    }

    @PostMapping(value = "/edit")
    public ResponseEntity<?> editUser(@RequestBody User user) {
        return userService.update(user);
    }

    @PostMapping(value = "/activate")
    public ResponseEntity<?> activateByOtp(@RequestParam String loginId,@RequestParam String otp){
        return userService.activate(loginId,otp);
    }
    @PostMapping(value = "/otp/reset")
    public ResponseEntity<?> otpReset(@RequestParam String loginId){
        return userService.otpReset(loginId);
    }
    @GetMapping(value = "/list/active")
    public ResponseEntity<?> activeUserList(){
        return userService.findActiveUsers();
    }

    @PostMapping(value = "/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestParam String password){
        return userService.verifyPassword(password);
    }
    @PostMapping(value = "/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String workspaceId){
        return userService.resetPassword(email, workspaceId);
    }
    @PostMapping(value = "/change-password")
    public ResponseEntity<?> changePassword(@RequestParam String password,
                                            @RequestParam(required = false) Boolean resetTokens){
        return userService.changePassword(password, resetTokens);
    }
    @PostMapping(value = "/create-user")
    public ResponseEntity<?> createUser(@RequestBody UserInfoDto user, HttpServletRequest request){
        return userService.createUser(user, request);
    }
    @PostMapping(value = "/update-user")
    public ResponseEntity<?> updateUser(@RequestBody User user, HttpServletRequest request){
        return userService.updateUser(user, request);
    }
    @PostMapping(value = "/resetPassword/requestOTP")
    public ResponseEntity<?> resetPasswordRequestOTP(@RequestParam String email){
        return userService.resetPasswordRequestOTP(email);
    }
    @PostMapping(value = "/resetPassword/verifyOTP")
    public ResponseEntity<?> resetPasswordVerifyOTP(@RequestParam String email,@RequestParam String otp){
        return userService.resetPasswordVerifyOTP(email,otp);
    }
    @PostMapping(value = "/resetPassword/updatePassword")
    public ResponseEntity<?> resetPasswordUpdatePassword(@RequestParam String email,
                                                         @RequestParam String password,
                                                         @RequestParam String token, HttpServletRequest request){
        return userService.resetPasswordUpdatePassword(email,password,token);
    }
    @GetMapping("/list")
    public ResponseEntity<?> findAllActiveUsers(){
        return userService.findAllActiveUsers();
    }

}
