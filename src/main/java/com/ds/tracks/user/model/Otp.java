package com.ds.tracks.user.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "generated_otp")
public class Otp {
    @Id
    private String id;
    private String otp;
    private String loginId;
    private String reason;
    private String requestToken;
}
