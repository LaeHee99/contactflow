package com.challenge40x.contactflow.intake;

import java.util.Optional;

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
}