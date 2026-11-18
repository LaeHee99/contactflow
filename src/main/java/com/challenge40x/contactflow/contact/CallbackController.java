package com.challenge40x.contactflow.contact;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations/{consultationId}/callback")
public class CallbackController {

  private final CallbackService callbackService;

  public CallbackController(CallbackService callbackService) {
    this.callbackService = callbackService;
  }

  // 재연락 예약 생성·갱신 결과 반환
  @PutMapping
  public ResponseEntity<Void> scheduleCallback(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody CallbackRequest request) {
    var result = callbackService.schedule(
        consultationId,
        request.consented(),
        request.reason(),
        request.scheduledAt(),
        request.channel(),
        request.assignee(),
        request.changedBy(),
        request.changeReason()
    );

    if (result.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.status(result.get() ? HttpStatus.CREATED : HttpStatus.OK).build();
  }

  // 철회 요청 처리 및 예약 부재 시 404 반환
  @PutMapping("/consent")
  public ResponseEntity<Void> withdrawConsent(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody ConsentRequest request) {
    if (request.consented()) {
      return ResponseEntity.badRequest().build();
    }

    return callbackService.withdrawConsent(consultationId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  // 모의 연락 시작 성공 시 204 반환
  @PutMapping("/start")
  public ResponseEntity<Void> startCallback(
      @PathVariable("consultationId") long consultationId) {
    return callbackService.start(consultationId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  // 재연락 미응답 기록 성공 시 204, 대상 부재 시 404 반환
  @PutMapping("/attempt")
  public ResponseEntity<Void> recordCallbackAttempt(
      @PathVariable("consultationId") long consultationId,
      @Valid @RequestBody CallbackAttemptRequest request) {
    return callbackService.recordNoAnswer(consultationId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  // 재연락 미응답 결과 입력 검증
  public record CallbackAttemptRequest(
      @NotNull @Pattern(regexp = "NO_ANSWER") String outcome
  ) {
  }

  // 재연락 동의 철회 입력
  public record ConsentRequest(@NotNull Boolean consented) {
  }

  // 재연락 동의 또는 작업 상태 충돌 응답
  @ExceptionHandler(ContactTaskConflictException.class)
  public ResponseEntity<Void> handleCallbackConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  // 재연락 동의·보류 사유·예약 정보 필수 입력
  public record CallbackRequest(
      @NotNull Boolean consented,
      @NotBlank @Size(max = 1000) String reason,
      @NotNull Instant scheduledAt,
      @NotBlank @Size(max = 32) String channel,
      @NotBlank @Size(max = 255) String assignee,
      @Size(max = 255) String changedBy,
      @Size(max = 1000) String changeReason
  ) {
  }
}