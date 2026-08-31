package org.example.smashhub.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @Size(min = 4, message = "USERNAME_INVALID")
    String username;

    @Email(message = "EMAIL_INVALID")
    @NotBlank(message = "EMAIL_INVALID")
    String email;

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    String confirmPassword;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "PHONE_INVALID")
    String phone;
}
