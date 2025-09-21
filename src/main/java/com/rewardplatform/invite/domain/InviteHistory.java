package com.rewardplatform.invite.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invite_histories", indexes = {
        @Index(name = "idx_ih_inviter", columnList = "inviter_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InviteHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;
}
