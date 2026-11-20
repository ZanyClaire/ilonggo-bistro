package com.ilonggobistro.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessEventRepository extends JpaRepository<BusinessEvent, Long> {

    @EntityGraph(attributePaths = "photos")
    List<BusinessEvent> findByPublishedTrueOrderByEventDateDescIdDesc();

    @EntityGraph(attributePaths = "photos")
    List<BusinessEvent> findByPublishedTrueAndEventTypeIgnoreCaseOrderByEventDateDescIdDesc(String eventType);

    @EntityGraph(attributePaths = "photos")
    List<BusinessEvent> findAllByOrderByEventDateDescIdDesc();

    @EntityGraph(attributePaths = "photos")
    Optional<BusinessEvent> findWithPhotosBySlugIgnoreCaseAndPublishedTrue(String slug);

    @EntityGraph(attributePaths = "photos")
    Optional<BusinessEvent> findWithPhotosById(Long id);

    boolean existsBySlug(String slug);
}
