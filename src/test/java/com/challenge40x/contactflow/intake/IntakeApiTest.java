package com.challenge40x.contactflow.intake;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.assertj.core.api.Assertions.assertThat;

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

  // 상담에 연결된 첫 연락 작업 수 조회
  private long countFirstContactTasks(JdbcTemplate jdbcTemplate, long consultationId) {
    return jdbcTemplate.queryForObject("""
        SELECT COUNT(*) FROM contact_task
        WHERE consultation_id = ? AND task_type = ?
        """, Long.class, consultationId, "FIRST_CONTACT");
  }
}