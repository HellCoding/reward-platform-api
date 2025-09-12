package com.rewardplatform.randombox.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prizes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Prize extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brand;
    private String description;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrizeType prizeType;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public enum PrizeType {
        GIFTICON, POINT, PRODUCT
    }
}
