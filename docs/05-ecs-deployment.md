# AWS ECS Fargate 배포 전략

## 개요

게임 리워드 플랫폼을 AWS ECS Fargate에 배포하는 전략과 인프라 비용 최적화 방안입니다.

## 아키텍처

```
┌─────────────────────────────────────────────┐
│                  AWS VPC                     │
│                                             │
│  ┌─────────┐    ┌─────────────────────┐     │
│  │   ALB   │───▶│   ECS Fargate       │     │
│  │         │    │  ┌───────────────┐  │     │
│  │ Health  │    │  │ Task 1 (Leader)│  │     │
│  │ Check   │    │  │ - API Server  │  │     │
│  │ /health │    │  │ - Scheduler   │  │     │
│  └─────────┘    │  └───────────────┘  │     │
│                 │  ┌───────────────┐  │     │
│                 │  │ Task 2        │  │     │
│                 │  │ - API Server  │  │     │
│                 │  └───────────────┘  │     │
│                 └─────────────────────┘     │
│                                             │
│  ┌──────────┐  ┌──────────┐                 │
│  │ RDS      │  │ ElastiC. │                 │
│  │ MySQL 8  │  │ Redis 7  │                 │
│  └──────────┘  └──────────┘                 │
└─────────────────────────────────────────────┘
```

## Auto Scaling 전략

### 스케일링 정책

| 조건 | 최소 | 최대 | 스케일 아웃 | 스케일 인 |
|------|------|------|-----------|---------|
| CPU 사용률 | 2대 | 4대 | 70% 이상 | 30% 이하 |
| Memory 사용률 | 2대 | 4대 | 80% 이상 | 40% 이하 |

### FARGATE_SPOT 비용 절약

```yaml
# ECS Task Definition
capacityProviderStrategy:
  - capacityProvider: FARGATE_SPOT
    weight: 3        # 70% SPOT
    base: 0
  - capacityProvider: FARGATE
    weight: 1        # 30% On-Demand (최소 안정성 보장)
    base: 1          # 최소 1대는 On-Demand
```

**비용 절감 효과**: FARGATE_SPOT은 On-Demand 대비 약 70% 할인

## Docker 설정

### Multi-stage Build

```dockerfile
# Build Stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
```

### Docker Compose (개발 환경)

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on: [mysql, redis]

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: reward_platform
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
    ports: ["3306:3306"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

## 모니터링

### Spring Actuator + Prometheus

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: reward-platform-api
```

### 핵심 메트릭

| 메트릭 | 설명 | 알림 임계값 |
|--------|------|-----------|
| `http_server_requests_seconds` | API 응답 시간 | p95 > 500ms |
| `jvm_memory_used_bytes` | JVM 메모리 | > 80% |
| `redis_command_duration` | Redis 레이턴시 | p99 > 100ms |
| `batch_execution_status` | 배치 성공/실패 | FAIL 발생 시 |

## CI/CD (GitHub Actions)

```
Push to main
  → Build & Test (Gradle)
  → Docker Build
  → Push to ECR
  → ECS Rolling Update (zero-downtime)
```

### Rolling Update 전략

```yaml
deploymentConfiguration:
  maximumPercent: 200          # 새 태스크 먼저 기동
  minimumHealthyPercent: 100   # 기존 태스크 유지
  deploymentCircuitBreaker:
    enable: true
    rollback: true             # 실패 시 자동 롤백
```
