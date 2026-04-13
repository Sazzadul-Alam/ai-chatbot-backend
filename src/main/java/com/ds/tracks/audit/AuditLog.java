package com.ds.tracks.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@Document("audit_log")
public class AuditLog {
    @Id
    private String id;
    private String loginId;
    private String ipAddress;
    private String requestUri;
    private String action;
    private String spaceId;
    private String spaceName;
    private String subspaceId;
    private String subspaceName;
    private String source;
    private String sourceId;
    private String sourceName;
    private Boolean isSourceDeleted;
    private LocalDateTime date;
    private Double latitude;
    private Double longitude;

    public AuditLog() {
    }

    public AuditLog(HttpServletRequest request) {
        this.ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (this.ipAddress == null) {
            this.ipAddress = request.getRemoteAddr();
        }
        this.requestUri = request.getRequestURI();
        this.date = LocalDateTime.now();
        if(Objects.nonNull(SecurityContextHolder.getContext()) && Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())){
            this.loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        }
    }

    public AuditLog(HttpServletRequest request, String action) {
        this.ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (this.ipAddress == null) {
            this.ipAddress = request.getRemoteAddr();
        }
        this.requestUri = request.getRequestURI();
        this.action = action;
        this.date = LocalDateTime.now();
        if(Objects.nonNull(SecurityContextHolder.getContext()) && Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())){
            this.loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        }
    }

}
