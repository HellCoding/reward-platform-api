package com.rewardplatform.user.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String name;
    private String phone;
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LoginProvider loginProvider;

    private String providerId;
    private int tokenVersion;
    private LocalDateTime lastLoginDate;

    public enum Role {
        USER, ADMIN, SUPER_ADMIN
    }

    public enum Status {
        ACTIVE, SUSPENDED, DORMANT, WITHDRAWN
    }

    public enum LoginProvider {
        KAKAO, APPLE
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    public void updateLastLogin() {
        this.lastLoginDate = LocalDateTime.now();
    }
}
