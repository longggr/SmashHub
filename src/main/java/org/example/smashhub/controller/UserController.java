package org.example.smashhub.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.ChangePasswordRequest;
import org.example.smashhub.dto.request.UserCreationRequest;
import org.example.smashhub.dto.request.UserUpdateRequest;
import org.example.smashhub.dto.response.PageResponse;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.service.AuthService;
import org.example.smashhub.service.UserService;
import org.example.smashhub.shared.response.ApiResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor // thay the cho autowired
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    AuthService authService;
    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request){
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userService.createUser(request))
                .message("User created successfully")
                .build();
    }
    @PutMapping("/{id}")
    ApiResponse<UserResponse> updateUser(@PathVariable Long id,
                                         @RequestBody UserUpdateRequest request){
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userService.updateUser(id, request))
                .build();

    }

    @GetMapping
    ApiResponse<List<UserResponse>> getAll(
            @RequestParam int pageNo,
            @RequestParam int pageSize
    ){
         return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAll(pageNo-1,pageSize))
                 .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id){
        userService.delete(id);
        return ApiResponse.<Void>builder()
                .message("user has been deleted")
                .build();

    }

    @GetMapping("/id/{id}")
    ApiResponse<UserResponse> getUserById(@PathVariable Long id){
        return ApiResponse.<UserResponse>builder()
                .result(userService.findUserById(id))
                .code(1000)
                .build();
    }

    @GetMapping("/email/{email}")
    ApiResponse<UserResponse> getUserByEmail(@PathVariable String email){
        return ApiResponse.<UserResponse>builder()
                .result(userService.findUserByEmail(email))
                .code(1000)
                .build();
    }
    @GetMapping("/search")
    public ApiResponse<PageResponse<UserResponse>> searchUsers(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "10", required = false) int size,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "ASC", required = false) Sort.Direction sortDirection){

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.searchUser(keyword, pageable))
                .code(1000)
                .build();
    }

    @PostMapping("/change-password")
    ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(email, request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Password changed successfully")
                .build();
    }
}
