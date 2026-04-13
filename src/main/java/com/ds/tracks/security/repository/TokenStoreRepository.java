package com.ds.tracks.security.repository;

import com.ds.tracks.security.model.TokenStore;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TokenStoreRepository extends MongoRepository<TokenStore, String> {
    TokenStore findByToken(String jwtToken);
    @Query(value = "{ 'token': ?0, 'loginId': ?1 }", fields = "{ 'status':1, 'id':0 }")
    TokenStore findStatusByTokenAndLoginId(String token, String loginId);

    int deleteByToken(String token);

    void deleteAllByLoginId(String loginId);
}
