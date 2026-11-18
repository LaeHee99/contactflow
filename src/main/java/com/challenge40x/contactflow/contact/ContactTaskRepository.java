package com.challenge40x.contactflow.contact;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactTaskRepository extends JpaRepository<ContactTask, Long> {
  // 상담별 연락 작업을 ID 오름차순으로 조회
  List<ContactTask> findAllByConsultation_IdOrderByIdAsc(long consultationId);

  // 배정·완료 요청이 겹칠 때 같은 작업을 순서대로 처리하도록 잠금 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ContactTask> findByIdAndConsultation_Id(long taskId, long consultationId);

  // 재연락 작업의 상태 변경을 위한 잠금 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ContactTask> findByConsultation_IdAndTaskType(long consultationId, ContactTask.TaskType taskType);

  // 상담별 기본 기한 이내 완료된 첫 연락 작업 수 집계
  @Query(value = """
      SELECT COUNT(*)
      FROM contact_task
      WHERE consultation_id = :consultationId
        AND task_type = 'FIRST_CONTACT'
        AND status = 'COMPLETED'
        AND completed_at <= due_at
      """, nativeQuery = true)
  long countOnTimeFirstContacts(@Param("consultationId") long consultationId);

  // 조회 대상 작업이 지정된 상담에 속하는지 확인
  boolean existsByIdAndConsultation_Id(long taskId, long consultationId);
}