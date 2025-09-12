package com.rewardplatform.randombox.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 랜덤박스 엔티티
 *
 * 티켓으로 구매하여 확률 기반 상품을 획득하는 컨텐츠.
 * BoxType에 따라 비용, 상품 구성, 참여 제한이 다름.
 */
@Entity
@Table(name = "random_boxes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RandomBox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BoxType boxType;

    @Column(nullable = false)
    private int ticketCost;

    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    private Integer reentryRestrictionDays;

    @OneToMany(mappedBy = "randomBox", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RandomBoxPrize> prizes = new ArrayList<>();

    public enum BoxType {
        BRONZE,   // 기본 박스
        SILVER,   // 프리미엄 박스
        WELCOME   // 신규 유저 전용 (7일간, 최대 1회)
    }
}
