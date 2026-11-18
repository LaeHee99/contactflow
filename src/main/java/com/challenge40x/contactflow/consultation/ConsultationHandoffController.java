package com.challenge40x.contactflow.consultation;

import com.challenge40x.contactflow.consultation.ConsultationHandoffService.HandoffView;
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

  public record HandoffRequest(
      @NotBlank @Size(max = 255) String gaAssignee,
      @NotBlank @Size(max = 255) String followUpOwner,
      @NotNull Instant nextCheckAt
  ) {
  }
}