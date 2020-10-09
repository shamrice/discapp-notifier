package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.ApplicationReportCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationReportCodeRepository extends JpaRepository<ApplicationReportCode, Long> {

    Optional<ApplicationReportCode> findByApplicationIdAndEmail(Long applicationId, String email);
}
