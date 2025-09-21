package com.rewardplatform.invite.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invite_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InviteCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
