package org.example.smashhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data // tu dong tao ra getter setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NotBlank(message = "username must not be blank")
    @Size(min = 4, max = 50,message = "username must be between 4 and 50 characters")
    String username;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    String email;

    @NotBlank(message = "password must be not blank")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
    @NotBlank(message = "password must be not blank")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String confirmPassword;

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Phone number must be a valid Vietnamese phone number")
    private String phone;
}
