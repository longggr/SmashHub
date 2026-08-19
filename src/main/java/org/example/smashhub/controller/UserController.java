package org.example.smashhub.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.UserCreationRequest;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.service.UserService;
import org.example.smashhub.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor // thay the cho autowired
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request){
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .result(userService.createUser(request))
                .message("User created successfully")
                .build();
    }

    @GetMapping
    ApiResponse<List<UserResponse>> getAll(){
         return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAll())
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
}
