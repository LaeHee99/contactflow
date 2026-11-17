package com.challenge40x.contactflow.intake;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeRepository extends JpaRepository<Intake, Long> {

  Optional<Intake> findBySourceAndExternalId(
      IntakeSource source, String externalId);
}