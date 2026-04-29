package com.audiocall.repository;

import com.audiocall.model.CallSession;
import com.audiocall.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {

    List<CallSession> findByStatusOrderByCreatedAtDesc(SessionStatus status);

    List<CallSession> findAllByOrderByCreatedAtDesc();

    Optional<CallSession> findByConferenceName(String conferenceName);

    List<CallSession> findByStatus(SessionStatus status);

    long countByStatus(SessionStatus status);
}
