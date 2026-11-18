## 1. 실행 환경

- Java 17
- PostgreSQL
- Git Bash
- curl

제출한 소스 ZIP을 압축 해제한 뒤, `build.gradle`과 `gradlew`가 있는 `contactflow` 폴더에서 시작합니다. Gradle은 프로젝트에 포함된 Wrapper를 사용합니다. 최초 실행 시 Gradle과 의존성을 내려받기 위해 인터넷 연결이 필요할 수 있습니다.

## 2. PostgreSQL 준비 및 앱 실행

### 데이터베이스 생성

PostgreSQL을 실행한 뒤, DataGrip 또는 `psql`에서 기본 `postgres` 데이터베이스에 접속합니다. 아래 SQL을 각각 실행합니다.

```sql
CREATE DATABASE contactflow_demo;
CREATE DATABASE contactflow_test;
```

- `contactflow_demo`는 애플리케이션 실행과 API 시연에 사용합니다.
- `contactflow_test`는 자동 테스트에 사용합니다. 테스트 중 테이블이 생성·삭제되므로 테스트 전용으로 사용합니다.

### 애플리케이션 실행

프로젝트 루트의 Git Bash에서 아래 명령을 실행합니다. PostgreSQL 계정이나 포트가 다르면 `DB_USERNAME`과 JDBC URL을 실제 설정에 맞게 바꿉니다.

```bash
export DB_USERNAME=postgres
read -rsp 'PostgreSQL password: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD

DB_URL='jdbc:postgresql://127.0.0.1:5432/contactflow_demo' \
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
./gradlew bootRun
```

### 접수 API 동작 확인

로그에 `Started ContactflowApplication`이 표시되면 앱을 켜 둔 상태로 **별도 Git Bash**를 엽니다. 아래 명령을 같은 내용으로 **두 번** 실행합니다.

```bash
curl -i http://localhost:8080/api/intakes \
  -H 'Content-Type: application/json' \
  -d '{"source":"WEBSITE","externalId":"WEB-SMOKE-001","customerKey":"CUSTOMER-SMOKE-001"}'
```

처음 사용하는 원본 ID라면 첫 요청은 `201 Created`, 재전송은 `200 OK`가 반환됩니다. 두 응답의 `intakeId`는 같습니다. 이미 시연한 DB에서 다시 확인하려면 `externalId`를 새 값으로 바꾼 뒤 같은 요청을 두 번 보냅니다.

## 3. 핵심 API 시나리오 재현

앱을 실행한 상태에서 **같은 Git Bash 창에서 P36 → P37 → P38 순서로** 진행합니다. P37과 P38은 P36에서 정한 상담 ID `C`와 `http` 함수를 사용합니다. `A`, `B`, `C`의 예시 숫자는 각 API 응답에서 받은 실제 ID로 바꿉니다.

### P36 · 누락된 홈페이지 원본을 기존 상담에 연결

**① 홈페이지와 메타 원본을 각각 접수합니다.**

같은 후보자가 홈페이지와 메타에서 각각 신청한 상황을 만듭니다.

```bash
BASE=http://localhost:8080

http() {
  local path="$1"
  shift
  curl -sS -i -H 'Content-Type: application/json' "$@" "$BASE$path"
}

RUN=$(date +%s)

http /api/intakes -X POST -d \
  "{\"source\":\"WEBSITE\",\"externalId\":\"WEB-$RUN\",\"customerKey\":\"C-$RUN\"}"

http /api/intakes -X POST -d \
  "{\"source\":\"META\",\"externalId\":\"META-$RUN\",\"customerKey\":\"C-$RUN\"}"
```

**② 메타 원본으로 상담을 만들고 홈페이지 원본의 상담 후보를 확인합니다.**

`A`와 `B`를 각 응답에서 받은 `intakeId`로 바꿉니다. 메타 원본 `B`로 상담을 만들고, 홈페이지 원본 `A`는 미연결 상태로 둡니다.

```bash
A=1  # 홈페이지 원본의 intakeId로 변경
B=2  # 메타 원본의 intakeId로 변경

http "/api/intakes/$B/consultation" -X PUT
http '/api/intakes?linked=false'
http "/api/intakes/$A/consultation-candidates"
```

미연결 목록에 `A`가 남아 있고, `A`의 상담 후보에 `B`로 만든 상담이 표시됩니다.

**③ 홈페이지 원본을 기존 상담에 연결하고 복구 요청을 반복합니다.**

직전 단계의 `PUT` 응답에서 받은 `consultationId`를 `C`에 넣고 실행합니다.

```bash
C=1  # B의 상담 연결 응답에서 받은 consultationId로 변경

http "/api/intakes/$A/consultation" -X PUT -d "{\"consultationId\":$C}"
http "/api/intakes/$A/consultation" -X PUT -d "{\"consultationId\":$C}"

http "/api/consultations/$C/contact-tasks"
http "/api/consultations/$C/metrics"
```

**기대 결과**

- `A`의 첫 연결은 `201`, 반복 요청은 `200`이며 같은 상담 ID가 반환됩니다.
- 두 원본이 한 상담에 연결되고, 첫 연락 작업은 한 건입니다.
- 집계는 `recoveredIntakeCount: 1`, `onTimeFirstContactCount: 0`입니다.

### P37 · GA 인수와 실제 연락 구분

**① GA 인계와 인수를 기록하고 확인 대상 목록을 조회합니다.**

같은 상담에 GA 인계와 인수 시각을 기록한 뒤, 확인 기한에 도달한 인계 목록을 조회합니다. 아래 시각은 가상 운영 기록입니다.

```bash
H="/api/consultations/$C/handoff"

http "$H" -X PUT -d '{
  "gaAssignee":"ga-001",
  "followUpOwner":"operator-001",
  "nextCheckAt":"2026-11-19T09:00:00Z"
}'

http "$H/acceptance" -X PUT -d '{"acceptedAt":"2026-11-19T08:00:00Z"}'
http "$H"
http '/api/consultations/handoffs?dueBy=2026-11-19T09:00:00Z'
```

**기대 결과**

- 최초 인계는 `201`, 인수 기록은 `204`입니다.
- 인계 상세는 `ACCEPTED` / `UNCONFIRMED`로 표시됩니다.
- 확인 대상 목록에 인톡 담당자와 다음 기한이 남습니다.

**② 실제 연락 근거를 기록하고 확인 대상 목록을 다시 조회합니다.**

```bash
http "$H/contact" -X PUT -d '{
  "contactedAt":"2026-11-19T09:15:00Z",
  "evidence":"Mock call connected"
}'

http '/api/consultations/handoffs?dueBy=2026-11-19T09:30:00Z'
```

**기대 결과**  
연락 기록은 `204`이며, 해당 상담은 확인 대상 목록에서 제외됩니다.

### P38 · 재연락 예약 후 동의 철회

같은 상담에 재연락을 예약하고, 후보자가 동의를 철회한 뒤 모의 시작을 요청합니다.

```bash
R="/api/consultations/$C/callback"

http "$R" -X PUT -d '{
  "consented":true,
  "reason":"Mock callback request",
  "scheduledAt":"2026-11-18T01:00:00Z",
  "channel":"PHONE",
  "assignee":"operator-001"
}'

http "$R/consent" -X PUT -d '{"consented":false}'
http "$R/start" -X PUT
http "/api/consultations/$C/contact-tasks"
```

**기대 결과**

- 최초 예약은 `201`, 동의 철회는 `204`, 모의 시작은 `409`가 반환됩니다.
- 작업 목록에서 `CALLBACK`은 `CANCELLED`이고 `startedAt`은 비어 있습니다.
- 실제 전화나 문자는 발송하지 않습니다.

## 4. 자동 테스트 실행

시연을 마친 뒤 앱을 실행했던 터미널에서 `Ctrl+C`를 누릅니다. DB 계정과 비밀번호 환경변수가 설정된 **같은 터미널**에서 아래 명령을 실행합니다.

```bash
DB_URL='jdbc:postgresql://127.0.0.1:5432/contactflow_test' \
./gradlew test --rerun-tasks
```

- HTML 보고서는 [`build/reports/tests/test/index.html`](build/reports/tests/test/index.html)에서 확인할 수 있습니다.
- 최종 제출 전 검증에서는 전체 27개 테스트가 통과했습니다. API 통합 20개, 접수 서비스 4개, 영업시간 계산 2개, 앱 초기화 1개입니다.
- 복구 도중 저장 실패, 주말·지정 휴일 기한 계산, 담당자 수락, 변경 이력, 동의 철회와 모의 시작의 동시 요청은 아래 **핵심 검증 시나리오**의 개별 테스트 명령으로 재현할 수 있습니다.
