package com.challenge40x.contactflow.consultation;

import com.challenge40x.contactflow.consultation.ConsultationHandoffService.DueHandoffView;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsultationHandoffListController {

  private final ConsultationHandoffService handoffService;

  public ConsultationHandoffListController(ConsultationHandoffService handoffService) {
    this.handoffService = handoffService;
  }

  // 기준 시각까지 확인이 필요한 GA 인계 목록 반환
  @GetMapping("/api/consultations/handoffs")
  public List<DueHandoffView> listDueHandoffs(@RequestParam("dueBy") Instant dueBy) {
    return handoffService.findDueHandoffs(dueBy);
  }
}