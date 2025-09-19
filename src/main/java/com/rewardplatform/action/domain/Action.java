package com.rewardplatform.action.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게임 액션 엔티티
 *
 * 출석체크, 가위바위보, 룰렛, 그림맞추기 등 8종 이상의 게임 액션 정의.
 * 일일 참여 횟수 제한과 성공/실패 보상을 설정합니다.
 */
@Entity
@Table(name = "actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Action extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionType actionType;

    @Column(nullable = false)
    @Builder.Default
    private int successReward = 1;

    @Column(nullable = false)
    @Builder.Default
    private int failReward = 0;

    @Column(nullable = false)
    @Builder.Default
    private int dailyLimit = 5;

    @Column(nullable = false)
    @Builder.Default
    private int maxDailyReward = 50;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private int ord = 0; // 메인 화면 노출 순서

    public enum ActionType {
        ATTENDANCE,         // 출석 체크
        RPS,               // 가위바위보
        PICTURE_MATCHING,  // 그림 맞추기
        ROULETTE,          // 룰렛
        STOPWATCH,         // 스톱워치
        FIND_CAT,          // 고양이 찾기
        AD_REWARDED,       // 리워드형 광고
        OMOK               // 오목
    }
}
