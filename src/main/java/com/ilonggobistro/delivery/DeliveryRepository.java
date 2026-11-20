package com.ilonggobistro.delivery;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @EntityGraph(attributePaths = {"order", "order.items"})
    Optional<Delivery> findWithOrderByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.items"})
    Optional<Delivery> findWithOrderById(Long id);

    @EntityGraph(attributePaths = {"order", "order.items"})
    Optional<Delivery> findWithOrderByOrderReceiptNumberIgnoreCase(String receiptNumber);

    @EntityGraph(attributePaths = {"order", "order.items"})
    Optional<Delivery> findWithOrderByExternalDeliveryId(String externalDeliveryId);

    @EntityGraph(attributePaths = {"order", "order.items"})
    List<Delivery> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"order", "order.items"})
    List<Delivery> findByStatusOrderByCreatedAtDesc(DeliveryStatus status);

    long countByStatusIn(List<DeliveryStatus> statuses);
}
