package com.audiocall.repository;

import com.audiocall.model.CallLog;
import com.audiocall.model.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    Optional<CallLog> findByCallSid(String callSid);

    List<CallLog> findBySessionId(Long sessionId);

    Page<CallLog> findBySessionId(Long sessionId, Pageable pageable);

    List<CallLog> findBySessionIdAndStatus(Long sessionId, CallStatus status);

    @Query("SELECT cl FROM CallLog cl WHERE cl.session.id = :sessionId AND " +
           "cl.status IN :statuses AND cl.retryCount < :maxRetries")
    List<CallLog> findRetryableCalls(@Param("sessionId") Long sessionId,
                                     @Param("statuses") List<CallStatus> statuses,
                                     @Param("maxRetries") int maxRetries);

    @Query("SELECT COUNT(cl) FROM CallLog cl WHERE cl.session.id = :sessionId AND cl.status = :status")
    long countBySessionIdAndStatus(@Param("sessionId") Long sessionId,
                                   @Param("status") CallStatus status);

    @Query("SELECT cl FROM CallLog cl WHERE " +
           "(:sessionId IS NULL OR cl.session.id = :sessionId) AND " +
           "(:status IS NULL OR cl.status = :status) AND " +
           "(:fromDate IS NULL OR cl.initiatedAt >= :fromDate) AND " +
           "(:toDate IS NULL OR cl.initiatedAt <= :toDate)")
    Page<CallLog> findFiltered(@Param("sessionId") Long sessionId,
                               @Param("status") CallStatus status,
                               @Param("fromDate") LocalDateTime fromDate,
                               @Param("toDate") LocalDateTime toDate,
                               Pageable pageable);

    @Query("SELECT COUNT(cl) FROM CallLog cl WHERE cl.initiatedAt >= :since")
    long countCallsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(cl) FROM CallLog cl WHERE cl.status = 'COMPLETED' AND cl.initiatedAt >= :since")
    long countCompletedCallsSince(@Param("since") LocalDateTime since);
}
