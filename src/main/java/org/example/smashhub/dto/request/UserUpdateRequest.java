package org.example.smashhub.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data // tu dong tao ra getter setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @Pattern(
            regexp = "^$|^[^\\s].*[^\\s]$",
            message = "Username không hợp lệ (Không được chứa khoảng trắng ở hai đầu)"
    )
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    String username;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Phone number must be a valid Vietnamese phone number")
    String phone;
}
