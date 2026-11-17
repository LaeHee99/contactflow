package com.challenge40x.contactflow.intake;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestController
@RequestMapping("/api/intakes")
public class IntakeController {

  private final IntakeService intakeService;

  public IntakeController(IntakeService intakeService) {
    this.intakeService = intakeService;
  }

  // 접수 결과에 따른 HTTP 상태와 접수 ID 반환
  @PostMapping
  public ResponseEntity<IntakeResponse> createIntake(@RequestBody IntakeRequest request) {
    var result = intakeService.receive(new ReceiveIntakeCommand(
        request.source(), request.externalId(), request.customerKey()));
    var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;

    return ResponseEntity.status(status).body(new IntakeResponse(result.intakeId()));
  }

  // 접수 내용 충돌을 HTTP 409 응답으로 변환
  @ExceptionHandler(IntakeConflictException.class)
  public ResponseEntity<Void> handleIntakeConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  public record IntakeRequest(IntakeSource source, String externalId, String customerKey) {
  }

  public record IntakeResponse(long intakeId) {
  }
}