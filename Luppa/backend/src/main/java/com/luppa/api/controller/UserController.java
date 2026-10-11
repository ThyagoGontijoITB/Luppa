package com.luppa.api.controller;

import com.luppa.api.dto.PageResponse;
import com.luppa.api.dto.UserCreateRequest;
import com.luppa.api.dto.UserResponse;
import com.luppa.api.dto.UserUpdateRequest;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable
    ) {
        return PageResponse.from(userService.search(search, role, status, pageable));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Integer id) {
        return userService.get(id);
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody UserCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return userService.create(request, authUser);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return userService.update(id, request, authUser);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, @AuthenticationPrincipal AuthUser authUser) {
        userService.delete(id, authUser);
    }
}
