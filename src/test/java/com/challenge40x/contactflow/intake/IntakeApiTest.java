package com.challenge40x.contactflow.intake;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class IntakeApiTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  // 신규 접수 요청의 생성 응답 및 접수 ID 반환 검증
  @Test
  void createsIntake() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(post("/api/intakes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "source": "WEBSITE",
                  "externalId": "WEB-API-001",
                  "customerKey": "CUSTOMER-API-001"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.intakeId").isNumber());
  }

  // 동일 접수 키의 고객 정보 불일치 시 409 응답 검증
  @Test
  void returnsConflictForDifferentCustomerOnSameIntakeKey() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var request = """
        {
          "source": "WEBSITE",
          "externalId": "WEB-API-CONFLICT-001",
          "customerKey": "%s"
        }
        """;

    mockMvc.perform(post("/api/intakes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.formatted("CUSTOMER-001")))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/intakes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.formatted("CUSTOMER-002")))
        .andExpect(status().isConflict());
  }

  // 공백으로만 구성된 원본 접수 ID의 400 응답 검증
  @Test
  void rejectsBlankExternalId() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(post("/api/intakes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "source": "WEBSITE",
                  "externalId": "   ",
                  "customerKey": "CUSTOMER-API-INVALID-001"
                }
                """))
        .andExpect(status().isBadRequest());
  }

  // 접수 경로 누락 시 400 응답 검증
  @Test
  void rejectsMissingSource() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(post("/api/intakes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "externalId": "WEB-MISSING-SOURCE-001",
                  "customerKey": "CUSTOMER-001"
                }
                """))
        .andExpect(status().isBadRequest());
  }

  // 공백 고객 식별자 입력 시 400 응답 검증
  @Test
  void rejectsBlankCustomerKey() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(post("/api/intakes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "source": "WEBSITE",
                  "externalId": "WEB-BLANK-CUSTOMER-001",
                  "customerKey": "   "
                }
                """))
        .andExpect(status().isBadRequest());
  }

  // 원본 접수의 상담 연결 재처리 시 기존 상담 ID 반환 검증
  @Test
  void linksIntakeToConsultationOnlyOnce() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    long intakeId = webApplicationContext.getBean(IntakeService.class)
        .receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-RECOVERY-001", "CUSTOMER-RECOVERY-001"))
        .intakeId();

    var firstResponse = mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.consultationId").isNumber())
        .andReturn()
        .getResponse();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consultationId").isNumber())
        .andExpect(result -> org.assertj.core.api.Assertions
            .assertThat(result.getResponse().getContentAsString())
            .isEqualTo(firstResponse.getContentAsString()));
  }

  // 담당자가 지정한 기존 상담에 미연결 원본을 연결하고 재처리 결과 검증
  @Test
  void recoversIntakeByLinkingToExistingConsultation() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    String customerKey = "CUSTOMER-CROSS-SOURCE-001";

    long websiteId = intakeService.receive(new ReceiveIntakeCommand(
        IntakeSource.WEBSITE, "WEB-CROSS-SOURCE-001", customerKey)).intakeId();
    long metaId = intakeService.receive(new ReceiveIntakeCommand(
        IntakeSource.META, "META-CROSS-SOURCE-001", customerKey)).intakeId();

    String existingLink = mockMvc.perform(put("/api/intakes/{intakeId}/consultation", metaId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.consultationId").isNumber())
        .andReturn()
        .getResponse()
        .getContentAsString();

    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);
    long consultationId = jdbcTemplate.queryForObject("""
     SELECT consultation_id FROM intake WHERE id = ?
     """, Long.class, metaId);

    assertThat(countFirstContactTasks(jdbcTemplate, consultationId)).isEqualTo(1L);

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc.perform(put("/api/intakes/{intakeId}/consultation", websiteId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(existingLink))
          .andExpect(attempt == 0 ? status().isCreated() : status().isOk())
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
              .content().json(existingLink));
    }

    assertThat(jdbcTemplate.queryForObject("""
     SELECT COUNT(*) FROM intake
     WHERE id IN (?, ?) AND consultation_id = ?
     """, Long.class, websiteId, metaId, consultationId)).isEqualTo(2L);

    assertThat(countFirstContactTasks(jdbcTemplate, consultationId)).isEqualTo(1L);

    // 반복 복구는 한 건으로 집계하고 미연락 작업은 제외
    mockMvc.perform(get("/api/consultations/{consultationId}/metrics", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recoveredIntakeCount").value(1))
        .andExpect(jsonPath("$.onTimeFirstContactCount").value(0));
  }

  // 신규 상담의 첫 연락 작업이 대기 상태로 조회되는지 검증
  @Test
  void listsPendingFirstContactTaskForNewConsultation() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-CONTACT-TASK-001", "CUSTOMER-CONTACT-TASK-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);
    long consultationId = jdbcTemplate.queryForObject("""
        SELECT consultation_id FROM intake WHERE id = ?
        """, Long.class, intakeId);

    mockMvc.perform(get("/api/consultations/{consultationId}/contact-tasks", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].taskId").isNumber())
        .andExpect(jsonPath("$[0].taskType").value("FIRST_CONTACT"))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  // 기본 연락 기한과 고객 희망 시각의 별도 저장 및 재조회 검증
  @Test
  void assignsFirstContactTaskWithDueAt() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-ASSIGNMENT-001", "CUSTOMER-ASSIGNMENT-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);
    long taskId = jdbcTemplate.queryForObject("""
        SELECT id FROM contact_task
        WHERE consultation_id = ? AND task_type = 'FIRST_CONTACT'
        """, Long.class, consultationId);

    mockMvc.perform(put(
            "/api/consultations/{consultationId}/contact-tasks/{taskId}/assignment",
            consultationId, taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "assignee": "operator-001",
                  "dueAt": "2026-11-19T02:00:00Z",
                  "customerPreferredAt": "2026-11-19T05:00:00Z"
                }
                """))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/consultations/{consultationId}/contact-tasks", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].assignee").value("operator-001"))
        .andExpect(jsonPath("$[0].dueAt").value("2026-11-19T02:00:00Z"))
        .andExpect(jsonPath("$[0].customerPreferredAt").value("2026-11-19T05:00:00Z"));
  }

  // 연락 완료 기록 저장 및 동일 재전송 허용·다른 내용 덮어쓰기 차단 검증
  @Test
  void completesAssignedContactTaskWithEvidence() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-COMPLETION-001", "CUSTOMER-COMPLETION-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);
    long taskId = jdbcTemplate.queryForObject("""
        SELECT id FROM contact_task
        WHERE consultation_id = ? AND task_type = 'FIRST_CONTACT'
        """, Long.class, consultationId);

    mockMvc.perform(put(
            "/api/consultations/{consultationId}/contact-tasks/{taskId}/assignment",
            consultationId, taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "assignee": "operator-001",
                  "dueAt": "2026-11-18T02:00:00Z"
                }
                """))
        .andExpect(status().isNoContent());

    mockMvc.perform(put(
            "/api/consultations/{consultationId}/contact-tasks/{taskId}/completion",
            consultationId, taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "completedAt": "2026-11-18T01:30:00Z",
                  "evidence": "전화 연결 후 고객에게 상담 절차 안내 완료"
                }
                """))
        .andExpect(status().isNoContent());

    // 동일한 완료 요청 재전송 허용 검증
    mockMvc.perform(put(
            "/api/consultations/{consultationId}/contact-tasks/{taskId}/completion",
            consultationId, taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "completedAt": "2026-11-18T01:30:00Z",
                  "evidence": "전화 연결 후 고객에게 상담 절차 안내 완료"
                }
                """))
        .andExpect(status().isNoContent());

    // 완료된 작업의 다른 시각·근거 덮어쓰기 거부 검증
    mockMvc.perform(put(
            "/api/consultations/{consultationId}/contact-tasks/{taskId}/completion",
            consultationId, taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "completedAt": "2026-11-18T03:00:00Z",
                  "evidence": "변경된 완료 기록"
                }
                """))
        .andExpect(status().isConflict());

    mockMvc.perform(get("/api/consultations/{consultationId}/contact-tasks", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$[0].completedAt").value("2026-11-18T01:30:00Z"))
        .andExpect(jsonPath("$[0].evidence").value("전화 연결 후 고객에게 상담 절차 안내 완료"));

    // 정상 접수의 기한 내 첫 연락은 연락 지표에만 반영
    mockMvc.perform(get("/api/consultations/{consultationId}/metrics", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recoveredIntakeCount").value(0))
        .andExpect(jsonPath("$.onTimeFirstContactCount").value(1));
  }

  // 미배정 연락 작업의 완료 거부 및 대기 상태 유지 검증
  @Test
  void rejectsCompletionOfUnassignedTask() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-UNASSIGNED-001", "CUSTOMER-UNASSIGNED-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);
    long taskId = jdbcTemplate.queryForObject("""
        SELECT id FROM contact_task
        WHERE consultation_id = ? AND task_type = 'FIRST_CONTACT'
        """, Long.class, consultationId);

    mockMvc.perform(put(
            "/api/consultations/{consultationId}/contact-tasks/{taskId}/completion",
            consultationId, taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "completedAt": "2026-11-18T01:30:00Z",
                  "evidence": "고객 연락 완료"
                }
                """))
        .andExpect(status().isConflict());

    mockMvc.perform(get("/api/consultations/{consultationId}/contact-tasks", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PENDING"))
        .andExpect(jsonPath("$[0].completedAt").doesNotExist())
        .andExpect(jsonPath("$[0].evidence").doesNotExist());
  }

  // 재연락 동의와 희망 시각을 받은 상담에 대기 작업 생성 검증
  @Test
  void schedulesCallbackWithConsent() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-CALLBACK-001", "CUSTOMER-CALLBACK-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);

    mockMvc.perform(put("/api/consultations/{consultationId}/callback", consultationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "consented": true,
                  "reason": "고객이 저녁에 다시 연락해 달라고 요청",
                  "scheduledAt": "2026-12-01T09:00:00Z",
                  "channel": "PHONE",
                  "assignee": "operator-001"
                }
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/consultations/{consultationId}/contact-tasks", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].taskType").value("FIRST_CONTACT"))
        .andExpect(jsonPath("$[1].taskType").value("CALLBACK"))
        .andExpect(jsonPath("$[1].status").value("PENDING"))
        .andExpect(jsonPath("$[1].scheduledAt").value("2026-12-01T09:00:00Z"))
        .andExpect(jsonPath("$[1].channel").value("PHONE"))
        .andExpect(jsonPath("$[1].assignee").value("operator-001"));
  }

  // 재연락 동의 철회 후 작업 취소 및 연락 시작 차단 검증
  @Test
  void blocksCallbackStartAfterConsentWithdrawal() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-WITHDRAWAL-001", "CUSTOMER-WITHDRAWAL-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);

    mockMvc.perform(put("/api/consultations/{consultationId}/callback", consultationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "consented": true,
                  "reason": "고객이 재연락 요청",
                  "scheduledAt": "2026-12-01T09:00:00Z",
                  "channel": "PHONE",
                  "assignee": "operator-001"
                }
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(put("/api/consultations/{consultationId}/callback/consent", consultationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"consented": false}
                """))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/api/consultations/{consultationId}/callback/start", consultationId))
        .andExpect(status().isConflict());

    mockMvc.perform(get("/api/consultations/{consultationId}/contact-tasks", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].taskType").value("FIRST_CONTACT"))
        .andExpect(jsonPath("$[0].status").value("PENDING"))
        .andExpect(jsonPath("$[1].taskType").value("CALLBACK"))
        .andExpect(jsonPath("$[1].status").value("CANCELLED"))
        .andExpect(jsonPath("$[1].startedAt").doesNotExist());
  }

  // GA 인수와 실제 연락의 분리 및 미연락 시 확인 책임 유지 검증
  @Test
  void tracksGaAcceptanceAndContactSeparately() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-HANDOFF-001", "CUSTOMER-HANDOFF-001"))
        .intakeId();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);

    mockMvc.perform(put("/api/consultations/{consultationId}/handoff", consultationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "gaAssignee": "ga-operator-001",
                  "followUpOwner": "operator-001",
                  "nextCheckAt": "2026-11-19T09:00:00Z"
                }
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/consultations/{consultationId}/handoff", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gaAssignee").value("ga-operator-001"))
        .andExpect(jsonPath("$.acceptanceStatus").value("PENDING"))
        .andExpect(jsonPath("$.contactStatus").value("UNCONFIRMED"))
        .andExpect(jsonPath("$.followUpOwner").value("operator-001"))
        .andExpect(jsonPath("$.nextCheckAt").value("2026-11-19T09:00:00Z"));

    // GA 인수 기록 후 실제 연락 미확인과 인톡 확인 책임 유지 검증
    mockMvc.perform(put("/api/consultations/{consultationId}/handoff/acceptance", consultationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"acceptedAt": "2026-11-19T07:00:00Z"}
                """))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/consultations/{consultationId}/handoff", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acceptanceStatus").value("ACCEPTED"))
        .andExpect(jsonPath("$.acceptedAt").value("2026-11-19T07:00:00Z"))
        .andExpect(jsonPath("$.contactStatus").value("UNCONFIRMED"))
        .andExpect(jsonPath("$.followUpOwner").value("operator-001"))
        .andExpect(jsonPath("$.nextCheckAt").value("2026-11-19T09:00:00Z"));

    // 실제 연락 시각과 근거 기록 후 연락 확인 상태 검증
    mockMvc.perform(put("/api/consultations/{consultationId}/handoff/contact", consultationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "contactedAt": "2026-11-19T08:00:00Z",
                  "evidence": "GA 담당자가 후보자와 통화해 상담 일정을 확인함"
                }
                """))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/consultations/{consultationId}/handoff", consultationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acceptanceStatus").value("ACCEPTED"))
        .andExpect(jsonPath("$.contactStatus").value("CONFIRMED"))
        .andExpect(jsonPath("$.contactedAt").value("2026-11-19T08:00:00Z"))
        .andExpect(jsonPath("$.contactEvidence")
            .value("GA 담당자가 후보자와 통화해 상담 일정을 확인함"));
  }

  // 미연결 원본 조회 및 상담 연결 후 목록 제외 검증
  @Test
  void listsUnlinkedIntakeUntilConsultationIsLinked() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-UNLINKED-001", "CUSTOMER-UNLINKED-001"))
        .intakeId();

    String matchingIntakes = "$[?(@.externalId == 'WEB-UNLINKED-001')]";

    // 연결 전 목록에 해당 홈페이지 원본이 한 건 있는지 검증
    mockMvc.perform(get("/api/intakes").param("linked", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(matchingIntakes + ".source")
            .value(org.hamcrest.Matchers.contains("WEBSITE")));

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/intakes").param("linked", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(matchingIntakes).isEmpty());
  }

  // 첫 연락 작업 저장 실패 시 상담 롤백 및 재시도 결과 검증
  @Test
  void retriesIntakeRecoveryAfterContactTaskInsertFails() throws Exception {
    var mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var intakeService = webApplicationContext.getBean(IntakeService.class);
    var jdbcTemplate = webApplicationContext.getBean(JdbcTemplate.class);

    long intakeId = intakeService.receive(new ReceiveIntakeCommand(
            IntakeSource.WEBSITE, "WEB-RECOVERY-RETRY-001", "CUSTOMER-RECOVERY-RETRY-001"))
        .intakeId();
    long consultationsBefore = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM consultation", Long.class);

    // 이 테스트에서만 신규 첫 연락 작업 저장 실패 유도
    jdbcTemplate.execute("""
        ALTER TABLE contact_task
        ADD CONSTRAINT reject_first_contact_for_recovery_test
        CHECK (task_type <> 'FIRST_CONTACT') NOT VALID
        """);
    try {
      assertThatThrownBy(() ->
          mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId)))
          .hasStackTraceContaining("reject_first_contact_for_recovery_test");
    } finally {
      jdbcTemplate.execute("""
          ALTER TABLE contact_task
          DROP CONSTRAINT reject_first_contact_for_recovery_test
          """);
    }

    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM consultation", Long.class)).isEqualTo(consultationsBefore);
    assertThat(jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId)).isNull();

    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isCreated());
    mockMvc.perform(put("/api/intakes/{intakeId}/consultation", intakeId))
        .andExpect(status().isOk());

    long consultationId = jdbcTemplate.queryForObject(
        "SELECT consultation_id FROM intake WHERE id = ?", Long.class, intakeId);

    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM consultation", Long.class)).isEqualTo(consultationsBefore + 1);
    assertThat(countFirstContactTasks(jdbcTemplate, consultationId)).isEqualTo(1L);
  }

  // 상담에 연결된 첫 연락 작업 수 조회
  private long countFirstContactTasks(JdbcTemplate jdbcTemplate, long consultationId) {
    return jdbcTemplate.queryForObject("""
        SELECT COUNT(*) FROM contact_task
        WHERE consultation_id = ? AND task_type = ?
        """, Long.class, consultationId, "FIRST_CONTACT");
  }
}