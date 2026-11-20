package com.ilonggobistro.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventPhotoRepository extends JpaRepository<EventPhoto, Long> {
    long countByApprovedFalse();
    Optional<EventPhoto> findByIdAndEventId(Long id, Long eventId);
}
