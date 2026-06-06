package com.labassist.security;

import com.labassist.audit.AuditService;
import com.labassist.audit.domain.AuditAction;
import com.labassist.common.exception.ConflictException;
import com.labassist.security.domain.AppUser;
import com.labassist.security.web.CreateUserRequest;
import com.labassist.security.web.UserInfo;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin user administration: create and list accounts. */
@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserInfo create(CreateUserRequest request, String actor, String ipAddress) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists: " + request.username());
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(request.role());
        user.setEnabled(true);
        AppUser saved = userRepository.save(user);

        auditService.success(AuditAction.USER_CREATE, actor, "AppUser", saved.getId().toString(),
                Map.of("username", saved.getUsername(), "role", saved.getRole().name()), ipAddress);
        return UserInfo.from(saved);
    }

    @Transactional(readOnly = true)
    public List<UserInfo> list() {
        return userRepository.findAll(Sort.by("username")).stream().map(UserInfo::from).toList();
    }
}
