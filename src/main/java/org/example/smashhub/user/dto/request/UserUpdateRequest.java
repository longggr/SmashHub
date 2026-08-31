package org.example.smashhub.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @Size(min = 4, message = "USERNAME_INVALID")
    String username;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "PHONE_INVALID")
    String phone;
}
