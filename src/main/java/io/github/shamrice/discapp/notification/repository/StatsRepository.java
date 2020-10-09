package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.Stats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<Stats, Long> {
    List<Stats> findByApplicationIdOrderByCreateDtDesc(long applicationId, Pageable pageable);

    List<Stats> findByApplicationIdAndCreateDtBetween(Long applicationId, Date creatDtStart, Date creatDtEnd);
}
