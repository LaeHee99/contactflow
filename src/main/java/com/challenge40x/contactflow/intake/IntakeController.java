package com.challenge40x.contactflow.intake;

import com.challenge40x.contactflow.intake.IntakeConsultationService.ConsultationCandidate;
import com.challenge40x.contactflow.intake.IntakeService.IntakeSummary;
import com.challenge40x.contactflow.intake.IntakeConsultationService.RecoveryConfirmation;
import java.util.List;
import java.time.Instant;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/intakes")
public class IntakeController {

  private final IntakeService intakeService;
  private final IntakeConsultationService intakeConsultationService;

  public IntakeController(
      IntakeService intakeService,
      IntakeConsultationService intakeConsultationService) {
    this.intakeService = intakeService;
    this.intakeConsultationService = intakeConsultationService;
  }

  // linked=false 조건의 미연결 접수 목록 반환
  @GetMapping(params = "linked=false")
  public List<IntakeSummary> listUnlinkedIntakes() {
    return intakeService.listUnlinked();
  }

  // 복구 대상 원본의 상담 후보 반환 및 원본 부재 시 404 응답
  @GetMapping("/{intakeId}/consultation-candidates")
  public ResponseEntity<List<ConsultationCandidate>> listConsultationCandidates(
      @PathVariable("intakeId") long intakeId) {
    return ResponseEntity.of(intakeConsultationService.findCandidates(intakeId));
  }

  // 접수 결과에 따른 HTTP 상태와 접수 ID 반환
  @PostMapping
  public ResponseEntity<IntakeResponse> createIntake(
      @Valid
      @RequestBody
      IntakeRequest request) {
    var result = intakeService.receive(new ReceiveIntakeCommand(
        request.source(),
        request.externalId(),
        request.customerKey(),
        request.receivedAt()));
    var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;

    return ResponseEntity.status(status).body(new IntakeResponse(result.intakeId()));
  }

  // 상담 연결 결과에 따라 201·200·404 응답 반환
  @PutMapping("/{intakeId}/consultation")
  public ResponseEntity<ConsultationLinkResponse> linkConsultation(
      @PathVariable("intakeId") long intakeId,
      @RequestParam(name = "recovery", defaultValue = "false") boolean recovery,
      @Valid @RequestBody(required = false) ConsultationLinkRequest request) {
    Long targetConsultationId = request == null ? null : request.consultationId();
    var result = intakeConsultationService.link(intakeId, targetConsultationId, recovery);
    if (result.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var link = result.get();
    var status = link.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status)
        .body(new ConsultationLinkResponse(link.consultationId()));
  }

  // 복구 확인 저장 성공 시 204, 원본 부재 시 404 반환
  @PutMapping("/{intakeId}/recovery-confirmation")
  public ResponseEntity<Void> confirmRecovery(
      @PathVariable("intakeId") long intakeId,
      @Valid @RequestBody RecoveryConfirmationRequest request) {
    boolean confirmed = intakeConsultationService.confirmRecovery(
        intakeId, request.confirmedBy(), request.confirmedAt());

    return confirmed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // 저장된 복구 확인 기록 반환
  @GetMapping("/{intakeId}/recovery-confirmation")
  public ResponseEntity<RecoveryConfirmation> getRecoveryConfirmation(
      @PathVariable("intakeId") long intakeId) {
    return ResponseEntity.of(intakeConsultationService.findRecoveryConfirmation(intakeId));
  }

  // 복구 확인 담당자와 확인 시각 필수 입력
  public record RecoveryConfirmationRequest(
      @NotBlank @Size(max = 255) String confirmedBy,
      @NotNull Instant confirmedAt
  ) {
  }

  // 접수 내용 충돌을 HTTP 409 응답으로 변환
  @ExceptionHandler(IntakeConflictException.class)
  public ResponseEntity<Void> handleIntakeConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  // 접수 경로 필수 및 원본 접수 ID·고객 식별자 공백 검증
  public record IntakeRequest(
      @NotNull IntakeSource source,
      @NotBlank String externalId,
      @NotBlank String customerKey,
      Instant receivedAt
  ) {
  }

  public record IntakeResponse(long intakeId) {
  }

  // 담당자가 지정한 기존 상담 ID 입력
  public record ConsultationLinkRequest(@NotNull Long consultationId) {
  }

  public record ConsultationLinkResponse(long consultationId) {
  }
}