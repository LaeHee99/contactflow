package com.challenge40x.contactflow.contact;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactTaskCompletionService {

  private final ContactTaskRepository contactTaskRepository;

  public ContactTaskCompletionService(ContactTaskRepository contactTaskRepository) {
    this.contactTaskRepository = contactTaskRepository;
  }

  // 상담 내 연락 작업의 실제 완료 기록 저장
  @Transactional
  public boolean complete(
      long consultationId, long taskId, Instant completedAt, String evidence) {
    var task = contactTaskRepository.findByIdAndConsultation_Id(taskId, consultationId);
    if (task.isEmpty()) {
      return false;
    }

    task.get().complete(completedAt, evidence);
    return true;
  }
}