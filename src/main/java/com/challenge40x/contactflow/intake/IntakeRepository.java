package com.challenge40x.contactflow.intake;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntakeRepository extends JpaRepository<Intake, Long> {

  Optional<Intake> findBySourceAndExternalId(IntakeSource source, String externalId);

  // 원본 접수와 수신 시각 저장 및 재전송 시 최초 기록 유지
  @Modifying
  @Query(value = """
      INSERT INTO intake (source, external_id, customer_key, received_at)
      VALUES (:source, :externalId, :customerKey, :receivedAt)
      ON CONFLICT (source, external_id) DO NOTHING
      """, nativeQuery = true)
  int insertIfAbsent(
      @Param("source") String source,
      @Param("externalId") String externalId,
      @Param("customerKey") String customerKey,
      @Param("receivedAt") Instant receivedAt);

  // 상담 연결 중 동일 원본의 동시 처리 방지를 위한 행 잠금
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Intake i where i.id = :intakeId")
  Optional<Intake> findByIdForUpdate(@Param("intakeId") long intakeId);

  // 대상 상담에 같은 고객의 원본이 이미 연결되어 있는지 확인
  boolean existsByConsultation_IdAndCustomerKey(long consultationId, String customerKey);

  // 상담에 연결되지 않은 원본을 접수 ID 순으로 조회
  List<Intake> findAllByConsultationIsNullOrderByIdAsc();

  // 기존 상담 연결로 복구된 원본 접수 수 집계
  long countByConsultation_IdAndRecoveredTrue(long consultationId);

  // 같은 고객의 원본이 연결된 상담에서 대기 중인 첫 연락 작업 조회
  @Query(value = """
      SELECT task.consultation_id AS "consultationId",
             task.id AS "firstContactTaskId",
             task.status AS "firstContactStatus"
      FROM contact_task task
      WHERE task.task_type = 'FIRST_CONTACT'
        AND task.status = 'PENDING'
        AND EXISTS (
          SELECT 1
          FROM intake linked
          WHERE linked.consultation_id = task.consultation_id
            AND linked.customer_key = :customerKey
        )
      ORDER BY task.consultation_id, task.id
      """, nativeQuery = true)
  List<ConsultationCandidateRow> findConsultationCandidates(
      @Param("customerKey") String customerKey);

  interface ConsultationCandidateRow {
    Long getConsultationId();
    Long getFirstContactTaskId();
    String getFirstContactStatus();
  }
}