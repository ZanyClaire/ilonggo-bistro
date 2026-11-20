package com.ilonggobistro.pos;

import com.ilonggobistro.kitchen.KitchenStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PosOrderRepository extends JpaRepository<PosOrder, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<PosOrder> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    Optional<PosOrder> findWithItemsByReceiptNumberIgnoreCase(String receiptNumber);

    @EntityGraph(attributePaths = "items")
    Optional<PosOrder> findWithItemsByClientOrderUuid(String clientOrderUuid);

    boolean existsByReceiptNumber(String receiptNumber);

    @EntityGraph(attributePaths = "items")
    List<PosOrder> findTop6ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT o FROM PosOrder o
            WHERE (:query IS NULL
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(o.customerPhone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(o.receiptNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(o.offlineReceiptNumber, '')) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
              AND (:status IS NULL OR o.status = :status)
              AND (:fromTime IS NULL OR o.createdAt >= :fromTime)
              AND (:toTime IS NULL OR o.createdAt <= :toTime)
            ORDER BY o.createdAt DESC
            """)
    List<PosOrder> searchHistory(
            @Param("query") String query,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("status") PosOrderStatus status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @Query("""
            SELECT COALESCE(SUM(o.totalPhp), 0)
            FROM PosOrder o
            WHERE o.status = :status
              AND o.createdAt >= :start
              AND o.createdAt < :end
            """)
    BigDecimal sumRevenueBetween(
            @Param("status") PosOrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COUNT(o)
            FROM PosOrder o
            WHERE o.status = :status
              AND o.createdAt >= :start
              AND o.createdAt < :end
            """)
    long countOrdersBetween(
            @Param("status") PosOrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COALESCE(SUM(o.totalPhp), 0) FROM PosOrder o WHERE o.status = :status")
    BigDecimal sumAllRevenue(@Param("status") PosOrderStatus status);

    long countByStatus(PosOrderStatus status);

    @EntityGraph(attributePaths = "items")
    List<PosOrder> findByStatusAndKitchenStatusOrderByCreatedAtAscIdAsc(
            PosOrderStatus status,
            KitchenStatus kitchenStatus
    );

    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT o FROM PosOrder o
            WHERE o.status = :status
              AND o.kitchenStatus = :kitchenStatus
            ORDER BY COALESCE(o.kitchenStartedAt, o.createdAt) ASC, o.id ASC
            """)
    List<PosOrder> findCookingQueue(
            @Param("status") PosOrderStatus status,
            @Param("kitchenStatus") KitchenStatus kitchenStatus
    );

    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT o FROM PosOrder o
            WHERE o.status = :status
              AND o.kitchenStatus = :kitchenStatus
            ORDER BY COALESCE(o.kitchenReadyAt, o.createdAt) ASC, o.id ASC
            """)
    List<PosOrder> findReadyQueue(
            @Param("status") PosOrderStatus status,
            @Param("kitchenStatus") KitchenStatus kitchenStatus
    );

    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT o FROM PosOrder o
            WHERE o.status = :status
              AND o.kitchenStatus = :kitchenStatus
              AND (o.kitchenCompletedAt >= :startOfDay OR o.createdAt >= :startOfDay)
            ORDER BY COALESCE(o.kitchenCompletedAt, o.createdAt) DESC, o.id DESC
            """)
    List<PosOrder> findServedToday(
            @Param("status") PosOrderStatus status,
            @Param("kitchenStatus") KitchenStatus kitchenStatus,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    long countByStatusAndKitchenStatus(PosOrderStatus status, KitchenStatus kitchenStatus);
}
