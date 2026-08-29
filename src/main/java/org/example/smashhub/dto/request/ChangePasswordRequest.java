package org.example.smashhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {
    @NotBlank(message = "currentPassword must not be blank")
    String currentPassword;

    @NotBlank(message = "newPassword must not be blank")
    String newPassword;

    @NotBlank(message = "confirmNewPassword must not be blank")
    String confirmNewPassword;
}
