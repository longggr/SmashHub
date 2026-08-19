package org.example.smashhub.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.UserCreationRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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


    public List<UserResponse> getAll(){
        return userMapper.toUserResponseList(userRepository.findAll());
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
}
