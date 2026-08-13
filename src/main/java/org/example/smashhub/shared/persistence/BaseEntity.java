package org.example.smashhub.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends AbstractEntity {
    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;
    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
