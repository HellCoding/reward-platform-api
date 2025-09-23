package com.rewardplatform.common.batch;

import com.rewardplatform.common.lock.LeaderElectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 배치 실행 관리자
 *
 * 리더 선출 + 실행 이력 기록 + 중복 실행 방지를 통합 관리합니다.
 *
 * Flow:
 * 1. 리더 인스턴스 확인 (LeaderElectionService)
 * 2. 금일 중복 실행 확인
 * 3. IN_PROGRESS 이력 생성
 * 4. 배치 로직 실행
 * 5. SUCCESS/FAIL 기록
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchExecutionManager {

    private final LeaderElectionService leaderElectionService;
    private final BatchExecutionHistoryRepository historyRepository;

    /**
     * 배치 작업 실행 (리더 인스턴스에서만)
     *
     * @param jobName     배치 작업 이름
     * @param leaseTimeMs 리더 락 유지 시간
     * @param task        실행할 배치 로직 (처리 건수 반환)
     */
    public void executeIfLeader(String jobName, long leaseTimeMs, Supplier<BatchResult> task) {
        if (!leaderElectionService.tryBecomeLeader(jobName, leaseTimeMs)) {
            log.debug("Not leader for batch job '{}'. Skipping.", jobName);
            return;
        }

        // 금일 이미 성공한 실행이 있는지 확인
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        if (historyRepository.existsByJobNameAndStatusAndStartedAtAfter(
                jobName, BatchExecutionHistory.BatchStatus.SUCCESS, todayStart)) {
            log.info("Batch job '{}' already completed today. Skipping.", jobName);
            return;
        }

        BatchExecutionHistory history = BatchExecutionHistory.builder()
                .jobName(jobName)
                .instanceId(leaderElectionService.getInstanceId())
                .startedAt(LocalDateTime.now())
                .build();
        historyRepository.save(history);

        try {
            BatchResult result = task.get();
            history.markSuccess(result.processedCount(), result.summary());
            log.info("Batch job '{}' completed - processed: {}, time: {}ms",
                    jobName, result.processedCount(), history.getExecutionTimeMs());
        } catch (Exception e) {
            history.markFail(truncate(e.getMessage(), 2000));
            log.error("Batch job '{}' failed", jobName, e);
        } finally {
            historyRepository.save(history);
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "Unknown error";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public record BatchResult(int processedCount, String summary) {
        public static BatchResult of(int count, String summary) {
            return new BatchResult(count, summary);
        }
    }
}
