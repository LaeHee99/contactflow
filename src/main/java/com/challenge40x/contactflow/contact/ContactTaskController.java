package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.contact.ContactTaskQueryService.ContactTaskView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations/{consultationId}/contact-tasks")
public class ContactTaskController {

  private final ContactTaskQueryService contactTaskQueryService;
  private final ContactTaskAssignmentService contactTaskAssignmentService;

  public ContactTaskController(
      ContactTaskQueryService contactTaskQueryService,
      ContactTaskAssignmentService contactTaskAssignmentService) {
    this.contactTaskQueryService = contactTaskQueryService;
    this.contactTaskAssignmentService = contactTaskAssignmentService;
  }

  // 상담별 연락 작업 목록 반환 및 미등록 상담에 404 응답
  @GetMapping
  public ResponseEntity<List<ContactTaskView>> listContactTasks(
      @PathVariable("consultationId") long consultationId) {
    return ResponseEntity.of(contactTaskQueryService.findByConsultation(consultationId));
  }

  // 담당자·기한 저장 성공 시 204, 상담에 해당 작업이 없으면 404 반환
  @PutMapping("/{taskId}/assignment")
  public ResponseEntity<Void> assignContactTask(
      @PathVariable("consultationId") long consultationId,
      @PathVariable("taskId") long taskId,
      @Valid @RequestBody AssignmentRequest request) {
    boolean assigned = contactTaskAssignmentService.assign(
        consultationId, taskId, request.assignee(), request.dueAt());

    return assigned ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // 담당자 및 기본 연락 기한 필수 입력
  public record AssignmentRequest(@NotBlank String assignee, @NotNull Instant dueAt) {
  }
}