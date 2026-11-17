package com.challenge40x.contactflow.intake;

// 동일 접수 키의 요청 내용 불일치
public class IntakeConflictException extends RuntimeException {

  public IntakeConflictException() {
    super("Intake key already exists with different data");
  }
}