package org.example.smashhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.shared.enums.AuthProvider;
import org.example.smashhub.shared.enums.Status;
import org.example.smashhub.shared.persistence.BaseEntity;


import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(nullable = false,unique = true, length = 100)
    String username;
    @Column(nullable = false,unique = true)
    String email;
    @Column(nullable = false)
    String password;
    @Column(length = 30)
    String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_name", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;
    /**
     * null = chưa xác thực email. Có giá trị = thời điểm xác thực thành công.
     * Set giá trị này ngay sau bước verify OTP thành công (xem luồng Redis OTP).
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

}
