package com.ds.tracks.user.repository;

import com.ds.tracks.user.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RoleRepository extends MongoRepository<Role, String> {
    List<Role> findAllByIdInAndStatus(List<String> ids, String status);
}
