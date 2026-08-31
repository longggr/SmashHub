package org.example.smashhub.user.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.AuthProvider;
import org.example.smashhub.common.enums.Status;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String username;
    String email;
    String phone;
    String roleName;
    Status status;
    AuthProvider authProvider;
    LocalDateTime emailVerifiedAt;
    LocalDateTime createDate;
    LocalDateTime updateDate;
}
