package com.example.meetingroom.repository;

import com.example.meetingroom.domain.Reservation;
import com.example.meetingroom.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        select (count(r) > 0) from Reservation r
        where r.room.id = :roomId
          and r.status in :statuses
          and r.startTime < :end
          and r.endTime > :start
        """)
    boolean existsOverlapping(
        @Param("roomId") Long roomId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("statuses") Collection<ReservationStatus> statuses
    );

    @EntityGraph(attributePaths = {"room", "requester", "reviewer"})
    List<Reservation> findByRoomIdOrderByStartTimeAsc(Long roomId);

    @EntityGraph(attributePaths = {"room", "requester", "reviewer"})
    List<Reservation> findByRoomNameIgnoreCaseOrderByStartTimeAsc(String roomName);

    @EntityGraph(attributePaths = {"room", "requester", "reviewer"})
    @Query("""
        select r from Reservation r
        where r.status = :status
          and r.startTime < :end
          and r.endTime > :start
        order by r.startTime, r.room.name
        """)
    List<Reservation> findTimeline(
        @Param("status") ReservationStatus status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @EntityGraph(attributePaths = {"room", "requester", "reviewer"})
    @Query("""
        select r from Reservation r
        where r.startTime >= :start and r.startTime < :end
          and (:status is null or r.status = :status)
        order by r.startTime, r.room.name
        """)
    List<Reservation> findMonthly(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("status") ReservationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"room", "requester", "reviewer"})
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
