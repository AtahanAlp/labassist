package com.labassist.security.web;

import com.labassist.common.web.RequestUtils;
import com.labassist.security.AppUserDetails;
import com.labassist.security.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only account administration (restricted to ROLE_ADMIN in SecurityConfig). */
@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List all accounts (admin only)")
    @GetMapping
    public List<UserInfo> list() {
        return userService.list();
    }

    @Operation(summary = "Create a new account (admin only)")
    @PostMapping
    public ResponseEntity<UserInfo> create(@Valid @RequestBody CreateUserRequest request,
                                           @AuthenticationPrincipal AppUserDetails principal,
                                           HttpServletRequest httpRequest) {
        UserInfo created = userService.create(request, principal.getUsername(), RequestUtils.clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
