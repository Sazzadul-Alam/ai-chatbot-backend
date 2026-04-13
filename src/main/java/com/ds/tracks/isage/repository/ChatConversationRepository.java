package com.ds.tracks.isage.repository;

import com.ds.tracks.isage.model.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {
    Optional<ChatConversation> findByLoginId(String loginId);
}