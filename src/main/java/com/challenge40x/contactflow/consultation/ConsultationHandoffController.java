package com.challenge40x.contactflow.consultation;

import com.challenge40x.contactflow.consultation.ConsultationHandoffService.HandoffView;
import com.challenge40x.contactflow.consultation.ConsultationHandoffService.UpdateResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations/{consultationId}/handoff")
public class ConsultationHandoffController {

  private final ConsultationHandoffService handoffService;

  public ConsultationHandoffController(ConsultationHandoffService handoffService) {
    this.handoffService = handoffService;
  }

  // 최초 GA 인계 등록 시 201, 기존 인계 갱신 시 204 반환
  @PutMapping
  public ResponseEntity<Void> recordHandoff(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody HandoffRequest request) {
    var created = handoffService.record(
        consultationId, request.gaAssignee(), request.followUpOwner(), request.nextCheckAt());

    if (created.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return created.get()
        ? ResponseEntity.status(HttpStatus.CREATED).build()
        : ResponseEntity.noContent().build();
  }

  // GA 인계 상태와 다음 확인 책임 반환
  @GetMapping
  public ResponseEntity<HandoffView> getHandoff(
      @PathVariable("consultationId") long consultationId) {
    return ResponseEntity.of(handoffService.find(consultationId));
  }

  // GA 인수 시각 기록
  @PutMapping("/acceptance")
  public ResponseEntity<Void> acceptHandoff(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody AcceptanceRequest request) {
    return toUpdateResponse(handoffService.accept(consultationId, request.acceptedAt()));
  }

  // GA 실제 연락 시각과 근거 기록
  @PutMapping("/contact")
  public ResponseEntity<Void> recordGaContact(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody ContactRequest request) {
    return toUpdateResponse(
        handoffService.recordContact(consultationId, request.contactedAt(), request.evidence()));
  }

  // GA 인계 후 인톡 확인 결과와 다음 기한 기록
  @PutMapping("/follow-up")
  public ResponseEntity<Void> recordFollowUp(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody FollowUpRequest request) {
    return toUpdateResponse(handoffService.recordFollowUp(
        consultationId, request.checkedAt(), request.nextCheckAt(), request.note()));
  }

  // 인계 기록 저장·부재·상태 충돌에 따른 HTTP 응답 반환
  private ResponseEntity<Void> toUpdateResponse(UpdateResult result) {
    return switch (result) {
      case SAVED -> ResponseEntity.noContent().build();
      case NOT_FOUND -> ResponseEntity.notFound().build();
      case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
    };
  }

  public record AcceptanceRequest(@NotNull Instant acceptedAt) {
  }

  public record ContactRequest(
      @NotNull Instant contactedAt,
      @NotBlank @Size(max = 1000) String evidence
  ) {
  }

  // 후속 확인 시각·다음 기한·메모 입력
  public record FollowUpRequest(
      @NotNull Instant checkedAt,
      @NotNull Instant nextCheckAt,
      @NotBlank @Size(max = 1000) String note
  ) {
  }

  public record HandoffRequest(
      @NotBlank @Size(max = 255) String gaAssignee,
      @NotBlank @Size(max = 255) String followUpOwner,
      @NotNull Instant nextCheckAt
  ) {
  }
}