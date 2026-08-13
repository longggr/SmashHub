package org.example.smashhub.entity;

import org.apache.catalina.User;
import org.example.smashhub.shared.persistence.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "addresses")
public class Address extends AbstractEntity {
    @Column(name = "recipient_name")
    String recipientName;
    String phone;
    @Column(nullable = false)
    String line1;
    String line2;

    @Column(nullable = false)
    String city;

    @Column(nullable = false)
    String province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;



}
