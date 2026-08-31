package org.example.smashhub.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    String currentPassword;

    @NotBlank(message = "FIELD_REQUIRED")
    String newPassword;

    @NotBlank(message = "FIELD_REQUIRED")
    String confirmNewPassword;
}
