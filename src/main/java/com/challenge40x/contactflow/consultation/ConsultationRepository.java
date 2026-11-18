package com.challenge40x.contactflow.consultation;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
  // 동일 상담의 재연락 예약 요청을 순서대로 처리하기 위한 잠금 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Consultation c where c.id = :consultationId")
  Optional<Consultation> findByIdForUpdate(@Param("consultationId") long consultationId);
}