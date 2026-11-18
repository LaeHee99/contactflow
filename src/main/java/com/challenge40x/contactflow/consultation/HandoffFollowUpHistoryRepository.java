package com.challenge40x.contactflow.consultation;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoffFollowUpHistoryRepository
    extends JpaRepository<HandoffFollowUpHistory, Long> {

  // 상담별 후속 조치 이력을 저장 순서로 조회
  List<HistoryRow> findAllByConsultation_IdOrderByIdAsc(long consultationId);

  interface HistoryRow {
    String getFollowUpOwner();
    Instant getCheckedAt();
    Instant getPreviousNextCheckAt();
    Instant getNextCheckAt();
    String getReason();
    String getNote();
  }
}