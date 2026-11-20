package com.ilonggobistro.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReferenceCodeIgnoreCase(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    List<Reservation> findByReservationDateOrderByReservationTimeAscCreatedAtAsc(LocalDate date);

    List<Reservation> findByReservationDateGreaterThanEqualOrderByReservationDateAscReservationTimeAsc(LocalDate fromDate);

    List<Reservation> findAllByOrderByReservationDateDescReservationTimeDesc();

    List<Reservation> findByStatusOrderByReservationDateAscReservationTimeAsc(ReservationStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.partySize), 0)
            FROM Reservation r
            WHERE r.reservationDate = :date
              AND r.reservationTime >= :windowStart
              AND r.reservationTime <= :windowEnd
              AND r.status IN :activeStatuses
            """)
    int sumSeatsInWindow(
            @Param("date") LocalDate date,
            @Param("windowStart") LocalTime windowStart,
            @Param("windowEnd") LocalTime windowEnd,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

    long countByReservationDateAndStatusNot(LocalDate date, ReservationStatus status);

    long countByStatus(ReservationStatus status);
}
