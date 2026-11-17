package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.contact.ContactTaskQueryService.ContactTaskView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations/{consultationId}/contact-tasks")
public class ContactTaskController {

  private final ContactTaskQueryService contactTaskQueryService;

  public ContactTaskController(ContactTaskQueryService contactTaskQueryService) {
    this.contactTaskQueryService = contactTaskQueryService;
  }

  // 상담별 연락 작업 목록 반환 및 미등록 상담에 404 응답
  @GetMapping
  public ResponseEntity<List<ContactTaskView>> listContactTasks(
      @PathVariable("consultationId") long consultationId) {
    return ResponseEntity.of(contactTaskQueryService.findByConsultation(consultationId));
  }
}