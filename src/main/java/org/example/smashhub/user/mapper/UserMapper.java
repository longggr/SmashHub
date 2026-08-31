package org.example.smashhub.user.mapper;

import org.example.smashhub.user.dto.request.UserCreationRequest;
import org.example.smashhub.user.dto.response.UserResponse;
import org.example.smashhub.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "role.name", target = "roleName")
    UserResponse toUserResponse(User user);

    @Mapping(target = "password", ignore = true)
    User toUser(UserCreationRequest request);

    List<UserResponse> toUserResponseList(List<User> users);
}
