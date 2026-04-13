package com.ds.tracks.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtTokenResponse {
    @JsonProperty("AccessToken")
    private final String accessToken;
    @JsonProperty("RefreshToken")
    private final String refreshToken;
    @JsonProperty("FullName")
    private final String fullName;
    @JsonProperty("Status")
    private final String status;
}
