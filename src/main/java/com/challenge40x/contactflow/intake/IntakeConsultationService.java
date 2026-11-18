package com.challenge40x.contactflow.intake;

import com.challenge40x.contactflow.contact.ContactTask;
import com.challenge40x.contactflow.contact.ContactTaskRepository;
import com.challenge40x.contactflow.consultation.Consultation;
import com.challenge40x.contactflow.consultation.ConsultationRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeConsultationService {

  private final IntakeRepository intakeRepository;
  private final ConsultationRepository consultationRepository;
  private final ContactTaskRepository contactTaskRepository;
  private final Set<LocalDate> holidays;

  public IntakeConsultationService(
      IntakeRepository intakeRepository,
      ConsultationRepository consultationRepository,
      ContactTaskRepository contactTaskRepository,
      @Value("${contactflow.holidays:}") String configuredHolidays) {
    this.intakeRepository = intakeRepository;
    this.consultationRepository = consultationRepository;
    this.contactTaskRepository = contactTaskRepository;
    this.holidays = Arrays.stream(configuredHolidays.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(LocalDate::parse)
        .collect(Collectors.toUnmodifiableSet());
  }

  // 새 상담 생성 또는 지정된 기존 상담 연결 및 재처리 결과 반환
  @Transactional
  public Optional<LinkResult> link(long intakeId, Long targetConsultationId, boolean recovery) {
    var intake = intakeRepository.findByIdForUpdate(intakeId);
    if (intake.isEmpty()) {
      return Optional.empty();
    }

    Intake storedIntake = intake.get();
    Consultation existing = storedIntake.getConsultation();
    if (existing != null) {
      if (targetConsultationId != null && !existing.getId().equals(targetConsultationId)) {
        throw new IntakeConflictException();
      }

      return Optional.of(new LinkResult(existing.getId(), false));
    }

    Consultation target;
    if (targetConsultationId == null) {
      target = consultationRepository.save(new Consultation());

      // 신규 상담의 첫 연락 작업 저장
      contactTaskRepository.save(
          ContactTask.firstContact(
              target,
              storedIntake.getReceivedAt(),
              holidays));
    } else {
      var requestedConsultation = consultationRepository.findById(targetConsultationId);
      if (requestedConsultation.isEmpty()) {
        return Optional.empty();
      }

      if (!intakeRepository.existsByConsultation_IdAndCustomerKey(
          targetConsultationId, storedIntake.getCustomerKey())) {
        throw new IntakeConflictException();
      }

      target = requestedConsultation.get();
    }

    // 명시적 복구 또는 기존 상담 연결의 복구 여부 기록
    storedIntake.linkConsultation(target, recovery || targetConsultationId != null);
    return Optional.of(new LinkResult(target.getId(), true));
  }

  public record LinkResult(long consultationId, boolean created) {
  }
}