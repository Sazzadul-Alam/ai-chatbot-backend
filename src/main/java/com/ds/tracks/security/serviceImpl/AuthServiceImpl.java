package com.ds.tracks.security.serviceImpl;

import com.ds.tracks.GeoLocationService;
import com.ds.tracks.commons.response.UserNameOrPasswordNotMatch;
import com.ds.tracks.security.*;
import com.ds.tracks.security.model.JwtUserDetails;
import com.ds.tracks.security.model.TokenStore;
import com.ds.tracks.security.repository.TokenStoreRepository;
import com.ds.tracks.security.service.AuthService;
import com.ds.tracks.audit.AuditLog;
import com.ds.tracks.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.util.Date;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    @Value("${jwt.http.request.header}")
    private String tokenHeader;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final TokenStoreRepository tokenStoreRepository;
    private final AuditLogRepository auditLogRepository;
    private final GeoLocationService geoLocationService;
    @Override
    public ResponseEntity<?> authenticate(String username, String password, HttpServletRequest request){
        JwtUserDetails userDetails = null;
        String ip=getClientIp(request);
        try{
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ue){
            return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDetails.getUsername(), password));
            final String token = jwtTokenUtil.generateToken(userDetails);
            final String refreshToken = jwtTokenUtil.refreshToken(token);
            tokenStoreRepository.save(
                    TokenStore.builder()
                            .loginId(username)
                            .token(token)
                            .status(userDetails.getStatus())
                            .generateDate(new Date())
                            .build()
            );
            AuditLog log = new AuditLog(request, "Logged In");
            log.setLoginId(username);
            log.setIpAddress(ip);
            double[] latLng = geoLocationService.getLatLng(ip);
            log.setLatitude(latLng[0]);
            log.setLongitude(latLng[1]);
            auditLogRepository.save(log);
            return new ResponseEntity<>(new JwtTokenResponse(token, refreshToken, userDetails.getFullName(), userDetails.getStatus()), HttpStatus.OK);
        } catch (AuthenticationException e){
            return new ResponseEntity<>("Invalid Credentials", HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0]; // real client IP
        }

        return request.getRemoteAddr();
    }
    @Override
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authToken = request.getHeader(tokenHeader);
        final String token = authToken.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        JwtUserDetails user = userDetailsService.manageCredentialByUsername(username);
        if (jwtTokenUtil.canTokenBeRefreshed(token)) {
            String refreshedToken = jwtTokenUtil.refreshToken(token);
            return ok(new JwtRefreshTokenResponse(refreshedToken,user.getFullName()));
        } else {
            return badRequest().body(null);
        }
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authToken = request.getHeader(tokenHeader);
        final String token = authToken.substring(7);
//        String username = jwtTokenUtil.getUsernameFromToken(token);
//        JwtUserDetails userDetails = userDetailsService.loadUserByUsername(username);
        int result = tokenStoreRepository.deleteByToken(token);
        auditLogRepository.save(new AuditLog(request, "Logged Out"));
        if (result ==1 ) {
            SecurityContextHolder.getContext().setAuthentication(null);
            return new ResponseEntity<>(new UserNameOrPasswordNotMatch(200, "Logout Successfully"),
                    OK);
        } else {
            return new ResponseEntity<>(new UserNameOrPasswordNotMatch(401, "Invalid Token"),
                    UNAUTHORIZED);
        }
    }
}
