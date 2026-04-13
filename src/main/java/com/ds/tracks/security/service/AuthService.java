package com.ds.tracks.security.service;

import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public interface AuthService {
    ResponseEntity<?> authenticate(String username, String password, HttpServletRequest request);

    ResponseEntity<?> refreshToken(HttpServletRequest request);

    ResponseEntity<?> logout(HttpServletRequest request);
}
