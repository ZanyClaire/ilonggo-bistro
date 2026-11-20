package com.ilonggobistro.kitchen;

import com.ilonggobistro.pos.PosOrder;
import com.ilonggobistro.pos.PosOrderItem;
import com.ilonggobistro.pos.PosOrderRepository;
import com.ilonggobistro.pos.PosOrderStatus;
import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KitchenService {

    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);

    private final PosOrderRepository orderRepository;
    private final Clock clock;

    public KitchenService(PosOrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public KitchenBoardSnapshot getBoardSnapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime startOfDay = LocalDate.now(clock).atStartOfDay();

        List<PosOrder> pending = orderRepository.findByStatusAndKitchenStatusOrderByCreatedAtAscIdAsc(
                PosOrderStatus.PAID, KitchenStatus.PENDING);
        List<PosOrder> cooking = orderRepository.findCookingQueue(
                PosOrderStatus.PAID, KitchenStatus.COOKING);
        List<PosOrder> ready = orderRepository.findReadyQueue(
                PosOrderStatus.PAID, KitchenStatus.READY);
        List<PosOrder> servedToday = orderRepository.findServedToday(
                PosOrderStatus.PAID, KitchenStatus.SERVED, startOfDay);

        List<DishPrepCount> prepSummary = computeDishPrepSummary(pending, cooking);
        String pendingIdsCsv = pending.stream()
                .map(o -> String.valueOf(o.getId()))
                .collect(Collectors.joining(","));

        return new KitchenBoardSnapshot(
                pending,
                cooking,
                ready,
                servedToday,
                prepSummary,
                now,
                pendingIdsCsv
        );
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return orderRepository.countByStatusAndKitchenStatus(PosOrderStatus.PAID, KitchenStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countActiveQueue() {
        return orderRepository.countByStatusAndKitchenStatus(PosOrderStatus.PAID, KitchenStatus.PENDING)
                + orderRepository.countByStatusAndKitchenStatus(PosOrderStatus.PAID, KitchenStatus.COOKING)
                + orderRepository.countByStatusAndKitchenStatus(PosOrderStatus.PAID, KitchenStatus.READY);
    }

    @Transactional
    public PosOrder updateKitchenStatus(Long orderId, KitchenStatus targetStatus, String staffUsername) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target kitchen status is required.");
        }
        PosOrder order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Kitchen ticket #" + orderId + " was not found."));

        if (order.getStatus() == PosOrderStatus.VOIDED) {
            throw new IllegalArgumentException("Voided orders cannot be updated on the Kitchen Display queue.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String actor = (staffUsername != null && !staffUsername.isBlank()) ? staffUsername.trim() : "kitchen";

        switch (targetStatus) {
            case PENDING -> {
                order.setKitchenStatus(KitchenStatus.PENDING);
                order.setKitchenCompletedAt(null);
            }
            case COOKING -> {
                order.setKitchenStatus(KitchenStatus.COOKING);
                if (order.getKitchenStartedAt() == null) {
                    order.setKitchenStartedAt(now);
                }
                order.setKitchenCompletedAt(null);
            }
            case READY -> {
                order.setKitchenStatus(KitchenStatus.READY);
                if (order.getKitchenStartedAt() == null) {
                    order.setKitchenStartedAt(now);
                }
                order.setKitchenReadyAt(now);
                order.setKitchenCompletedAt(null);
            }
            case SERVED -> {
                order.setKitchenStatus(KitchenStatus.SERVED);
                if (order.getKitchenStartedAt() == null) {
                    order.setKitchenStartedAt(now);
                }
                if (order.getKitchenReadyAt() == null) {
                    order.setKitchenReadyAt(now);
                }
                order.setKitchenCompletedAt(now);
            }
        }

        order.setKitchenUpdatedBy(actor);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public PosOrder getOrderForKot(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Kitchen ticket #" + orderId + " was not found."));
    }

    private List<DishPrepCount> computeDishPrepSummary(List<PosOrder> pending, List<PosOrder> cooking) {
        Map<String, PrepAccumulator> grouped = new LinkedHashMap<>();

        for (PosOrder order : pending) {
            for (PosOrderItem item : order.getItems()) {
                String cleanName = item.getItemName() != null ? item.getItemName().trim() : "Dish";
                String key = cleanName.toLowerCase(Locale.ENGLISH);
                PrepAccumulator acc = grouped.computeIfAbsent(key, k -> new PrepAccumulator(cleanName));
                acc.pendingQty += item.getQuantity();
            }
        }

        for (PosOrder order : cooking) {
            for (PosOrderItem item : order.getItems()) {
                String cleanName = item.getItemName() != null ? item.getItemName().trim() : "Dish";
                String key = cleanName.toLowerCase(Locale.ENGLISH);
                PrepAccumulator acc = grouped.computeIfAbsent(key, k -> new PrepAccumulator(cleanName));
                acc.cookingQty += item.getQuantity();
            }
        }

        return grouped.values().stream()
                .map(acc -> new DishPrepCount(
                        acc.displayName,
                        acc.pendingQty + acc.cookingQty,
                        acc.pendingQty,
                        acc.cookingQty
                ))
                .sorted(Comparator.comparingInt(DishPrepCount::totalQuantity).reversed()
                        .thenComparing(DishPrepCount::itemName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static final class PrepAccumulator {
        final String displayName;
        int pendingQty;
        int cookingQty;

        PrepAccumulator(String displayName) {
            this.displayName = displayName;
        }
    }

    public record DishPrepCount(
            String itemName,
            int totalQuantity,
            int pendingQuantity,
            int cookingQuantity
    ) {
    }

    public record KitchenBoardSnapshot(
            List<PosOrder> pendingOrders,
            List<PosOrder> cookingOrders,
            List<PosOrder> readyOrders,
            List<PosOrder> servedTodayOrders,
            List<DishPrepCount> dishPrepSummary,
            LocalDateTime snapshotTime,
            String pendingIdsCsv
    ) {
        public int pendingCount() {
            return pendingOrders.size();
        }

        public int cookingCount() {
            return cookingOrders.size();
        }

        public int readyCount() {
            return readyOrders.size();
        }

        public int servedCount() {
            return servedTodayOrders.size();
        }

        public int activeTotalCount() {
            return pendingOrders.size() + cookingOrders.size() + readyOrders.size();
        }

        public int totalPrepDishes() {
            return dishPrepSummary.stream().mapToInt(DishPrepCount::totalQuantity).sum();
        }

        public String formattedSnapshotTime() {
            return snapshotTime != null ? snapshotTime.format(CLOCK_FMT) : "";
        }
    }
}
