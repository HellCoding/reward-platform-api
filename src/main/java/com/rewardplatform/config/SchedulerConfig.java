package com.rewardplatform.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 스케줄러 설정
 *
 * ECS 다중 인스턴스 환경에서 스케줄러 실행 여부를 제어합니다.
 * scheduler.enabled=false로 설정하면 해당 인스턴스에서는 배치 작업이 실행되지 않습니다.
 * Redis 리더 선출과 조합하여 하나의 인스턴스만 배치를 실행합니다.
 */
@Configuration
@Getter
@Slf4j
public class SchedulerConfig {

    @Value("${scheduler.enabled:true}")
    private boolean enabled;

    public boolean shouldExecute() {
        return enabled;
    }
}
