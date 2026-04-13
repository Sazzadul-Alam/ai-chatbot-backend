package com.ds.tracks.security.serviceImpl;

import com.ds.tracks.security.model.JwtUserDetails;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.ds.tracks.commons.utils.UserStatus.ACTIVE;
import static com.ds.tracks.commons.utils.UserStatus.ONBOARDING;
import static java.lang.String.format;
import static org.springframework.http.ResponseEntity.ok;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {
    private final UserService userService;

    public JwtUserDetails manageCredentialByUsername(String username) {
        User userCredentials=userService.findUserByLoginId(username);
        if (Objects.nonNull(userCredentials)) {
            return new JwtUserDetails(1L, userCredentials.getLoginId(), userCredentials.getPassword(), userCredentials.getFullName(),"ROLE_USER_2");
        }else {
            return new JwtUserDetails(null, null, null, null);
        }
    }

    @Override
    public JwtUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findFirstByLoginIdAndStatusIn(username, Arrays.asList(ONBOARDING, ACTIVE));
        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException(username);
        }
        return new JwtUserDetails(1L, user.getLoginId(), user.getPassword(), user.getFullName(), user.getStatus());
    }
}
