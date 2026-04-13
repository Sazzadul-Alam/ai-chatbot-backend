package com.ds.tracks.security;

import com.ds.tracks.security.model.JwtUserDetails;
import com.ds.tracks.security.model.TokenStore;
import com.ds.tracks.security.repository.TokenStoreRepository;
import com.ds.tracks.audit.AuditLog;
import com.ds.tracks.audit.AuditLogRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static com.ds.tracks.commons.utils.UserStatus.ACTIVE;
import static com.ds.tracks.commons.utils.UserStatus.ONBOARDING;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenAuthorizationOncePerRequestFilter extends OncePerRequestFilter {

    private final TokenStoreRepository tokenStoreRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            try {
                jwtToken = requestTokenHeader.substring(7);
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (ExpiredJwtException e) {
                sendError(request, response, "Token Expired");
                return;
            } catch (Exception e){
                sendError(request, response, "Invalid Token");
                return;
            }
        }
        if (username != null && getContext().getAuthentication() == null) {
            TokenStore tokenStore = tokenStoreRepository.findStatusByTokenAndLoginId(jwtToken, username);
            if(Objects.isNull(tokenStore)){
                sendError(request, response, "Invalid Token");
                return;
            }
            if(!Arrays.asList(ONBOARDING, ACTIVE).contains(tokenStore.getStatus())){
                sendError(request, response, "User Inactive");
                return;
            }
            UserDetails userDetails = new JwtUserDetails(1L, username, null, null, tokenStore.getStatus());
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            usernamePasswordAuthenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            getContext().setAuthentication(usernamePasswordAuthenticationToken);
//            saveAuditLog(request);
        }
        chain.doFilter(request, response);
    }

    void sendError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }


    void saveAuditLog(HttpServletRequest request){
        try{
            if(request.getRequestURI().contains("invite")
                    || request.getRequestURI().contains("update")
                    || request.getRequestURI().contains("create")
                    || request.getRequestURI().contains("save")
                    || request.getRequestURI().contains("edit")
                    || request.getRequestURI().contains("change")
                    || request.getRequestURI().contains("upload")

                    || request.getRequestURI().contains("delete")
                    || request.getRequestURI().contains("remove")
                    || request.getRequestURI().contains("revoke")

                    || request.getRequestURI().contains("entry")

            ){
                auditLogRepository.save(new AuditLog(request));
            }
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
        }
    }
}
