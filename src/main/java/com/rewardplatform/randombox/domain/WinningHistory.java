package com.rewardplatform.randombox.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "winning_histories", indexes = {
        @Index(name = "idx_wh_user", columnList = "user_id"),
        @Index(name = "idx_wh_box_prize", columnList = "random_box_id, random_box_prize_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WinningHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id", nullable = false)
    private RandomBox randomBox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_prize_id")
    private RandomBoxPrize randomBoxPrize;

    private int pointsAwarded;
    private int ticketCost;

    @Column(length = 500)
    private String prizeName;
}
