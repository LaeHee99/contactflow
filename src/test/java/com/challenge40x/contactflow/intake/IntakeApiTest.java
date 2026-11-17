package com.challenge40x.contactflow.intake;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
}