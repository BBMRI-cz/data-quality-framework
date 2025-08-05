package eu.bbmri_eric.quality.agent.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, String> {}
