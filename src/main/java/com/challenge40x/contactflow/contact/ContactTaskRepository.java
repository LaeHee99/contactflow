package com.challenge40x.contactflow.contact;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactTaskRepository extends JpaRepository<ContactTask, Long> {
  // 상담별 연락 작업을 ID 오름차순으로 조회
  List<ContactTask> findAllByConsultation_IdOrderByIdAsc(long consultationId);

  // 배정·완료 요청이 겹칠 때 같은 작업을 순서대로 처리하도록 잠금 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ContactTask> findByIdAndConsultation_Id(long taskId, long consultationId);

  // 재연락 작업의 상태 변경을 위한 잠금 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ContactTask> findByConsultation_IdAndTaskType(long consultationId, ContactTask.TaskType taskType);
}