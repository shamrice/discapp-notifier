package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {

    List<Thread> getThreadByApplicationIdAndDeletedAndIsApprovedAndCreateDtBetweenOrderByCreateDtAsc(Long applicationId, Boolean deleted, Boolean isApproved, Date createDateStart, Date createDateEnd);
    List<Thread> getThreadByApplicationIdAndDeletedAndIsApprovedAndParentIdAndCreateDtBetweenOrderByCreateDtAsc(Long applicationId, Boolean deleted, Boolean isApproved, Long parentId, Date createDateStart, Date createDateEnd);

    long countByApplicationIdAndIsApprovedAndDeleted(Long applicationId, Boolean isApproved, Boolean deleted);
    Thread findTopByApplicationIdAndDeletedOrderByCreateDt(Long applicationId, Boolean deleted);
}
