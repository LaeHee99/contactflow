package com.challenge40x.contactflow.contact;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactTaskRepository extends JpaRepository<ContactTask, Long> {
}