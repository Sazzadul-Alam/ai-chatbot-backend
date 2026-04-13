package com.ds.tracks.user.serviceImpl;

import com.ds.tracks.user.model.dto.UserPermissionDto;
import com.ds.tracks.user.repository.UsersPermissionRepository;
import com.ds.tracks.user.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {
    private final UsersPermissionRepository usersPermissionRepository;

    @Override
    public ResponseEntity<?> updateCapacity(UserPermissionDto userPermissionDto) {
        return ResponseEntity.ok().body(usersPermissionRepository.updateCapacity(userPermissionDto));
    }
}
