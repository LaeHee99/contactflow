# ContactFlow

홈페이지·메타 접수를 상담에 연결하고, 첫 안내부터 GA 인계와 재연락까지 연락 약속을 관리하는 Java API입니다. 가상 데이터로 접수 누락 복구, 연락 확인, 동의 철회 이후 실행 차단을 재현합니다.

## 실행 환경

- Java 17
- Spring Boot 4.1.2
- PostgreSQL
- 프로젝트에 포함된 Gradle Wrapper 사용

## 실행 방법

PostgreSQL에서 시연용 DB를 생성합니다. 이미 있으면 생성을 건너뜁니다.

```sql
CREATE DATABASE contactflow_demo;
```

프로젝트 루트의 Git Bash에서 실행합니다. `DB_USERNAME`은 로컬 PostgreSQL 계정에 맞게 변경합니다.

```bash
export DB_USERNAME=postgres
read -rsp 'PostgreSQL 비밀번호: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD

DB_URL='jdbc:postgresql://127.0.0.1:5432/contactflow_demo' \
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
./gradlew bootRun
```

다른 터미널에서 아래 명령을 두 번 실행합니다.

```bash
curl -i -X POST http://localhost:8080/api/intakes \
  -H 'Content-Type: application/json' \
  -d '{"source":"WEBSITE","externalId":"WEB-SMOKE-001","customerKey":"CUSTOMER-SMOKE-001"}'
```

빈 DB에서 첫 요청은 `201`, 동일 요청을 재전송하면 `200`과 같은 `intakeId`를 반환합니다. 이미 시연에 사용한 DB라면 `externalId`를 새 값으로 바꿔 확인할 수 있습니다.

## 테스트 실행

테스트 전용 DB `contactflow_test`를 생성합니다. 테스트 프로필은 이 DB의 테스트 테이블을 실행 시 생성하고 종료 시 삭제합니다.

```sql
CREATE DATABASE contactflow_test;
```

DB 계정·비밀번호 환경변수를 설정한 Git Bash에서 실행합니다.

```bash
DB_URL='jdbc:postgresql://127.0.0.1:5432/contactflow_test' \
./gradlew test --rerun-tasks
```

HTML 보고서는 `build/reports/tests/test/index.html`에서 확인할 수 있습니다.

## 핵심 검증 시나리오

아래 메서드는 모두 `IntakeApiTest`에 있습니다. 개별 시나리오는 `./gradlew test --tests '*IntakeApiTest.메서드명'`으로 실행할 수 있습니다.

| 범위 | 검증한 동작 | 테스트 메서드 |
| --- | --- | --- |
| P36 | 홈페이지 원본의 기존 상담 후보를 조회하고 연결합니다. 복구를 반복해도 두 원본은 보존되고 첫 연락 작업은 한 건입니다. | `recoversIntakeByLinkingToExistingConsultation` |
| P36 | 첫 연락 작업 저장 실패 시 상담을 롤백합니다. 재시도 후 복구 집계와 담당자 확인 기록을 조회합니다. | `retriesIntakeRecoveryAfterContactTaskInsertFails` |
| P37 | 접수 시각부터 첫 안내 기한을 계산합니다. | `calculatesFirstContactDueAtWhenAssignmentOmitsDeadline` |
| P37 | 기본 기한·고객 희망·합의 시각을 구분하고, 담당자 수락과 기한 변경 이력을 확인합니다. | `assignsFirstContactTaskWithDueAt` |
| P37 | GA 미수락과 인수 후 미연락을 구분하고, 후속 조치 이력과 확인 대상 목록을 검증합니다. | `distinguishesUnacceptedAndUncontactedGaFollowUps` |
| P38 | 예약 전 시작을 차단하고, 채널 변경 후 같은 작업 ID와 변경 이력을 확인합니다. | `schedulesCallbackWithConsent` |
| P38 | 미응답을 연락 완료와 구분하고, 동의 철회 후 연락 시작을 거부합니다. | `recordsUnansweredCallbackWithoutCompletingContact`, `blocksCallbackStartAfterConsentWithdrawal` |
| P38 | 철회와 시작을 동시에 요청했을 때 관찰된 처리 순서와 최종 상태가 일치하는지 확인합니다. | `keepsCallbackConsistentWhenWithdrawalAndStartRace` |

## 검증 결과와 시연 범위

- 빈 시연 DB에서 신규 접수 `201`, 동일 접수 재전송 `200`, 동일 `intakeId`를 확인했습니다.
- 전체 테스트 27개가 통과했습니다. API 20개, 접수 서비스 4개, 영업시간 계산 2개, 앱 초기화 1개입니다.
- 복구된 원본 수와 팀 기본 기한 안에 완료한 첫 연락 수는 별도로 집계합니다.
- 영업시간은 한국시간 평일 09:00~18:00입니다. 지정 휴일은 `CONTACTFLOW_HOLIDAYS`에 `YYYY-MM-DD` 날짜를 쉼표로 구분해 설정할 수 있습니다.
- 접수와 연락은 가상 시나리오입니다. 실서비스 연동, 전화·문자 발송, 로그인·권한 관리는 시연 범위 밖입니다. 담당자 식별자는 모의 입력값이며 실제 고객 대기시간이나 매출 효과는 측정하지 않았습니다.