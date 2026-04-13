package com.ds.tracks.isage.repository;

import com.ds.tracks.isage.model.ChatRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRequestRepository extends MongoRepository<ChatRequest, String> {
}