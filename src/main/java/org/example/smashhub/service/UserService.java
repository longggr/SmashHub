package org.example.smashhub.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.UserCreationRequest;
import org.example.smashhub.dto.request.UserUpdateRequest;
import org.example.smashhub.dto.response.PageResponse;
import org.example.smashhub.dto.response.UserResponse;
import org.example.smashhub.entity.Role;
import org.example.smashhub.entity.User;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.mapper.UserMapper;
import org.example.smashhub.repository.RoleRepository;
import org.example.smashhub.repository.UserRepository;
import org.example.smashhub.shared.enums.AuthProvider;
import org.example.smashhub.shared.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor // thay the cho autowired
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    private static final String DEFAULT_ROLE = "CUSTOMER";
    @Transactional
    public UserResponse createUser(UserCreationRequest request){
        if(!passwordEncoder.matches(request.getPassword(), request.getConfirmPassword()))
            throw new  AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        User user = userMapper.toUser(request);
        if(userRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTED);
        if(userRepository.existsByPhone(request.getPhone()))
            throw  new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        Role role = roleRepository.findById(DEFAULT_ROLE)
                .orElseThrow(() ->new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(Status.INACTIVE);
        user.setAuthProvider(AuthProvider.LOCAL);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (!(request.getUsername()==null)){
            if (userRepository.existsByUsername(request.getUsername()))
                throw new AppException(ErrorCode.USERNAME_EXISTED);
            user.setUsername(request.getUsername().trim());
        }
        if (!(request.getUsername()==null)){
            user.setUsername(request.getUsername().trim());
        }
        return userMapper.toUserResponse(userRepository.save(user));
    }


    public List<UserResponse> getAll(int pageNo,int pageSize){
        Pageable pageable = PageRequest.of(pageNo,pageSize);
        return userMapper.toUserResponseList(userRepository.findAll(pageable).getContent());
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setStatus(Status.LOCKED);
        userRepository.save(user);
    }

    public UserResponse findUserById(Long id){
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    public UserResponse findUserByEmail(String email) {
        return userMapper.toUserResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED)));
    }
    public PageResponse<UserResponse> searchUser(String keyword, Pageable pageable){
        Page<User> page = userRepository.searchUsers(keyword, pageable);
        List<UserResponse> userResponses = page.getContent()
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .content(userResponses)
                .build();

    }
}
