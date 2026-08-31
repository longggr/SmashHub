package org.example.smashhub.user.service;

import org.example.smashhub.common.dto.PageResponse;
import org.example.smashhub.user.dto.request.UserCreationRequest;
import org.example.smashhub.user.dto.request.UserUpdateRequest;
import org.example.smashhub.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreationRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    List<UserResponse> getAll(int pageNo, int pageSize);
    void delete(Long id);
    UserResponse findUserById(Long id);
    UserResponse findUserByEmail(String email);
    PageResponse<UserResponse> searchUser(String keyword, Pageable pageable);
}
