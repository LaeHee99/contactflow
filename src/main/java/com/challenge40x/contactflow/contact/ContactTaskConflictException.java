package com.challenge40x.contactflow.contact;

// 연락 작업의 현재 상태와 요청 동작 간 충돌
public class ContactTaskConflictException extends RuntimeException {

  public ContactTaskConflictException(String message) {
    super(message);
  }
}