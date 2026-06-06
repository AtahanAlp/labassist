package com.labassist.security;

import com.labassist.audit.AuditService;
import com.labassist.audit.domain.AuditAction;
import com.labassist.security.domain.AppUser;
import com.labassist.security.web.LoginRequest;
import com.labassist.security.web.LoginResponse;
import com.labassist.security.web.UserInfo;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/** Authenticates credentials, issues a JWT, and records the outcome to the audit log. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager, AppUserRepository userRepository,
                       JwtService jwtService, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            auditService.failure(AuditAction.LOGIN_FAILURE, request.username(), "AppUser", null,
                    Map.of("reason", ex.getClass().getSimpleName()), ipAddress);
            throw ex;
        }
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        String token = jwtService.generateToken(user);
        auditService.success(AuditAction.LOGIN_SUCCESS, user.getUsername(), "AppUser",
                user.getId().toString(), null, ipAddress);
        return LoginResponse.of(token, jwtService.expirationMinutes(), UserInfo.from(user));
    }
}
