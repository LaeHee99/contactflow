package com.challenge40x.contactflow.intake;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntakeRepository extends JpaRepository<Intake, Long> {

  Optional<Intake> findBySourceAndExternalId(IntakeSource source, String externalId);

  // 동일 접수 키 충돌 시 기존 원본 유지
  @Modifying
  @Query(value = """
        INSERT INTO intake (source, external_id, customer_key)
        VALUES (:source, :externalId, :customerKey)
        ON CONFLICT (source, external_id) DO NOTHING
        """, nativeQuery = true)
  int insertIfAbsent(
      @Param("source") String source,
      @Param("externalId") String externalId,
      @Param("customerKey") String customerKey);

  // 상담 연결 중 동일 원본의 동시 처리 방지를 위한 행 잠금
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Intake i where i.id = :intakeId")
  Optional<Intake> findByIdForUpdate(@Param("intakeId") long intakeId);

  // 대상 상담에 같은 고객의 원본이 이미 연결되어 있는지 확인
  boolean existsByConsultation_IdAndCustomerKey(long consultationId, String customerKey);

  // 상담에 연결되지 않은 원본을 접수 ID 순으로 조회
  List<Intake> findAllByConsultationIsNullOrderByIdAsc();
}