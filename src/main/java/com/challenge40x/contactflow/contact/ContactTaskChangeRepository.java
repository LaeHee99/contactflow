package com.challenge40x.contactflow.contact;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactTaskChangeRepository extends JpaRepository<ContactTaskChange, Long> {

  // 작업별 배정 변경 이력을 저장 순서대로 조회
  List<HistoryRow> findAllByTask_IdOrderByIdAsc(long taskId);

  interface HistoryRow {
    String getPreviousAssignee();
    String getNewAssignee();
    Instant getPreviousEffectiveDueAt();
    Instant getNewEffectiveDueAt();
    String getChangedBy();
    String getChangeReason();
    Instant getChangedAt();
  }
}