package com.ds.tracks.security.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@Document(collection = "token_store")
@NoArgsConstructor
@AllArgsConstructor
public class TokenStore {
    @Id
    private String id;
    private String token;
    private String loginId;
    private String status;
    private Date generateDate;
}
