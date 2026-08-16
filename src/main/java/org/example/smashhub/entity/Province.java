package org.example.smashhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "provinces")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Province {
    @Id
    @Column
    Integer id;

    @Column(nullable = false)
    String name;

}
