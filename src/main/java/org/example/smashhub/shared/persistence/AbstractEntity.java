package org.example.smashhub.shared.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true,nullable = false)
    private Long id;
}
