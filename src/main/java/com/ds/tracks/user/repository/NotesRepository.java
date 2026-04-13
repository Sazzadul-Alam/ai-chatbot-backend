package com.ds.tracks.user.repository;

import com.ds.tracks.user.model.Notes;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotesRepository extends MongoRepository<Notes, String> {

    Notes findFirstByUserId(String name);
}
