package com.rewardplatform.common.batch;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 배치 실행 이력 엔티티
 *
 * 모든 배치 작업의 실행 결과를 기록합니다.
 * - 실행 상태 (IN_PROGRESS, SUCCESS, FAIL)
 * - 처리 건수 및 소요 시간
 * - 실패 시 에러 메시지
 *
 * ECS 다중 인스턴스 환경에서 어떤 인스턴스가 배치를 실행했는지 추적합니다.
 */
@Entity
@Table(name = "batch_execution_history", indexes = {
        @Index(name = "idx_beh_job_status", columnList = "job_name, status"),
        @Index(name = "idx_beh_started_at", columnList = "started_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BatchExecutionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.IN_PROGRESS;

    @Column(nullable = false)
    private String instanceId;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Builder.Default
    private int processedCount = 0;

    @Builder.Default
    private int failedCount = 0;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 500)
    private String summary;

    public enum BatchStatus {
        IN_PROGRESS, SUCCESS, FAIL
    }

    public void markSuccess(int processedCount, String summary) {
        this.status = BatchStatus.SUCCESS;
        this.processedCount = processedCount;
        this.summary = summary;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFail(String errorMessage) {
        this.status = BatchStatus.FAIL;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }

    public long getExecutionTimeMs() {
        if (startedAt == null || finishedAt == null) return 0;
        return java.time.Duration.between(startedAt, finishedAt).toMillis();
    }
}
