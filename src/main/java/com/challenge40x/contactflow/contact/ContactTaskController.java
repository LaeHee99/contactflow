package com.challenge40x.contactflow.contact;

import com.challenge40x.contactflow.contact.ContactTaskQueryService.ContactTaskView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
  private final ContactTaskCompletionService contactTaskCompletionService;

  public ContactTaskController(
      ContactTaskQueryService contactTaskQueryService,
      ContactTaskAssignmentService contactTaskAssignmentService,
      ContactTaskCompletionService contactTaskCompletionService) {
    this.contactTaskQueryService = contactTaskQueryService;
    this.contactTaskAssignmentService = contactTaskAssignmentService;
    this.contactTaskCompletionService = contactTaskCompletionService;
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
        consultationId,
        taskId,
        request.assignee(),
        request.dueAt(),
        request.customerPreferredAt(),
        request.agreedContactAt());

    return assigned ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // 연락 작업 수락 성공 시 204, 대상 작업 부재 시 404 반환
  @PutMapping("/{taskId}/acceptance")
  public ResponseEntity<Void> acceptContactTask(
      @PathVariable("consultationId") long consultationId,
      @PathVariable("taskId") long taskId,
      @Valid @RequestBody AcceptanceRequest request) {
    boolean accepted = contactTaskAssignmentService.accept(
        consultationId, taskId, request.assignee(), request.acceptedAt());

    return accepted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // 연락 완료 기록 저장 후 204 또는 대상 작업 부재 시 404 반환
  @PutMapping("/{taskId}/completion")
  public ResponseEntity<Void> completeContactTask(
      @PathVariable("consultationId") long consultationId,
      @PathVariable("taskId") long taskId,
      @Valid @RequestBody CompletionRequest request) {
    boolean completed = contactTaskCompletionService.complete(
        consultationId, taskId, request.completedAt(), request.evidence());

    return completed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // 연락 작업 상태 충돌을 HTTP 409 응답으로 변환
  @ExceptionHandler(ContactTaskConflictException.class)
  public ResponseEntity<Void> handleContactTaskConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  // 연락 완료 시각과 근거 필수 입력
  public record CompletionRequest(
      @NotNull Instant completedAt,
      @NotBlank @Size(max = 1000) String evidence
  ) {
  }

  // 담당자 필수 입력 및 기본·희망·합의 시각 선택 입력
  public record AssignmentRequest(
      @NotBlank String assignee,
      Instant dueAt,
      Instant customerPreferredAt,
      Instant agreedContactAt
  ) {
  }

  // 수락 담당자와 수락 시각 입력
  public record AcceptanceRequest(@NotBlank String assignee, @NotNull Instant acceptedAt) {
  }
}