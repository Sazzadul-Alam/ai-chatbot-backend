package com.ds.tracks.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JwtRefreshTokenResponse {
    @JsonProperty("RefreshToken")
    private final String refreshToken;
    @JsonProperty("FullName")
    private final String fullName;
}
