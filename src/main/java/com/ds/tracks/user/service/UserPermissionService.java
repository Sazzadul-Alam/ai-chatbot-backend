package com.ds.tracks.user.service;

import com.ds.tracks.user.model.dto.UserPermissionDto;
import org.springframework.http.ResponseEntity;

public interface UserPermissionService {

    ResponseEntity<?> updateCapacity(UserPermissionDto userPermissionDto);
}
