package org.example.smashhub.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.shared.enums.AuthProvider;
import org.example.smashhub.shared.enums.Status;

import java.time.LocalDateTime;


@Data // tu dong tao ra getter setter
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
    LocalDateTime emailVerifiedAt;
    AuthProvider authProvider;
    LocalDateTime createDate;
}
