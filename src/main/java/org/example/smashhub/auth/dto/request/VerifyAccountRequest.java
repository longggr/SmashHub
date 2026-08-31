package org.example.smashhub.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyAccountRequest {
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    String email;

    @NotBlank(message = "otp must not be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "otp must be 6 digits")
    String otp;
}
