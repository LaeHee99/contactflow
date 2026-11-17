package com.challenge40x.contactflow.contact;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactTaskRepository extends JpaRepository<ContactTask, Long> {
  // 상담별 연락 작업을 ID 오름차순으로 조회
  List<ContactTask> findAllByConsultation_IdOrderByIdAsc(long consultationId);

  // 지정된 상담에 속한 연락 작업 조회
  Optional<ContactTask> findByIdAndConsultation_Id(long taskId, long consultationId);
}