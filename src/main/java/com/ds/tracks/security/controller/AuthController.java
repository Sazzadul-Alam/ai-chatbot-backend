package com.ds.tracks.security.controller;


import com.ds.tracks.commons.exception.AuthenticationException;
import com.ds.tracks.commons.response.UserNameOrPasswordNotMatch;
import com.ds.tracks.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
        return authService.authenticate(username, password, request);
    }
    @GetMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        return authService.refreshToken(request);
    }
    @DeleteMapping( "/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException e) {
        log.error("Token create exception: => {}", e.getMessage());
        return new ResponseEntity<>(new UserNameOrPasswordNotMatch(401, "Invalid User Password"),
                UNAUTHORIZED);
    }
}
