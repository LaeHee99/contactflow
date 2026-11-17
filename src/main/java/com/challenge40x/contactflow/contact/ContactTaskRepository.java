package com.challenge40x.contactflow.contact;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactTaskRepository extends JpaRepository<ContactTask, Long> {
  // 상담별 연락 작업을 ID 오름차순으로 조회
  List<ContactTask> findAllByConsultation_IdOrderByIdAsc(long consultationId);
}