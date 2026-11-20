package com.ilonggobistro.menu;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @EntityGraph(attributePaths = "category")
    List<MenuItem> findAllByOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "category")
    Optional<MenuItem> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    List<MenuItem> findByFeaturedTrueAndAvailableTrueOrderByDisplayOrderAscNameAsc();

    @Query("""
            SELECT m FROM MenuItem m
            JOIN FETCH m.category c
            WHERE (:categorySlug IS NULL OR c.slug = :categorySlug)
              AND (:query IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(m.badgeLabel, '')) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:onlyAvailable = FALSE OR m.available = TRUE)
            ORDER BY c.displayOrder ASC, m.displayOrder ASC, m.name ASC
            """)
    List<MenuItem> searchMenu(
            @Param("categorySlug") String categorySlug,
            @Param("query") String query,
            @Param("onlyAvailable") boolean onlyAvailable
    );

    long countByAvailableTrue();

    long countByAvailableFalse();

    long countByCategoryId(Long categoryId);
}
