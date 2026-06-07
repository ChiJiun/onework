package com.example.meetingroom.service;

import com.example.meetingroom.api.dto.CreateReservationRequest;
import com.example.meetingroom.api.dto.MonthlyReservationResponse;
import com.example.meetingroom.api.dto.ReservationResponse;
import com.example.meetingroom.api.dto.ReviewReservationRequest;
import com.example.meetingroom.api.dto.TimelineResponse;
import com.example.meetingroom.domain.Reservation;
import com.example.meetingroom.domain.ReservationStatus;
import com.example.meetingroom.domain.Room;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.domain.UserRole;
import com.example.meetingroom.exception.BusinessRuleException;
import com.example.meetingroom.exception.ReservationConflictException;
import com.example.meetingroom.exception.ResourceNotFoundException;
import com.example.meetingroom.repository.ReservationRepository;
import com.example.meetingroom.repository.RoomRepository;
import com.example.meetingroom.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingRoomService {

    private static final EnumSet<ReservationStatus> BLOCKING_STATUSES =
        EnumSet.of(ReservationStatus.PROCESSING, ReservationStatus.APPROVED);

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public MeetingRoomService(
        RoomRepository roomRepository,
        UserRepository userRepository,
        ReservationRepository reservationRepository
    ) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation reserveRoom(
        Long roomId, Long userId, LocalDateTime start, LocalDateTime end
    ) {
        CreateReservationRequest request = new CreateReservationRequest(
            roomId, userId, "Meeting", null, 1, start, end
        );
        return createReservation(request);
    }

    @Transactional
    public Reservation createReservation(CreateReservationRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        // Serializes reservation attempts per room; the DB exclusion constraint is the final safety net.
        Room room = roomRepository.findByIdForUpdate(request.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));
        User requester = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        if (!room.isActive()) {
            throw new BusinessRuleException("The room is not available");
        }
        if (request.attendeeCount() > room.getCapacity()) {
            throw new BusinessRuleException("Attendee count exceeds room capacity");
        }
        if (reservationRepository.existsOverlapping(
            room.getId(), request.startTime(), request.endTime(), BLOCKING_STATUSES
        )) {
            throw new ReservationConflictException("The room is already reserved for this time range");
        }

        Reservation reservation = new Reservation(
            room,
            requester,
            request.title().trim(),
            request.description(),
            request.attendeeCount(),
            request.startTime(),
            request.endTime()
        );
        return reservationRepository.saveAndFlush(reservation);
    }

    @Transactional
    public Reservation review(Long reservationId, ReviewReservationRequest request) {
        if (request.status() != ReservationStatus.APPROVED
            && request.status() != ReservationStatus.REJECTED) {
            throw new BusinessRuleException("Review status must be APPROVED or REJECTED");
        }
        if (request.status() == ReservationStatus.REJECTED
            && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessRuleException("A rejection reason is required");
        }

        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));
        User reviewer = userRepository.findById(request.reviewerId())
            .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found: " + request.reviewerId()));

        if (reviewer.getRole() != UserRole.REVIEWER && reviewer.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleException("The selected user is not allowed to review reservations");
        }
        if (request.status() == ReservationStatus.APPROVED) {
            reservation.approve(reviewer);
        } else {
            reservation.reject(reviewer, request.reason().trim());
        }
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByRoomId(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
        return reservationRepository.findByRoomIdOrderByStartTimeAsc(roomId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByRoomName(String roomName) {
        if (!roomRepository.existsByNameIgnoreCase(roomName)) {
            throw new ResourceNotFoundException("Room not found: " + roomName);
        }
        return reservationRepository.findByRoomNameIgnoreCaseOrderByStartTimeAsc(roomName);
    }

    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Reservation> reservations =
            reservationRepository.findTimeline(ReservationStatus.APPROVED, start, end);
        long roomCount = reservations.stream()
            .map(reservation -> reservation.getRoom().getId())
            .distinct()
            .count();
        return new TimelineResponse(
            date,
            roomCount,
            reservations.stream().map(ReservationResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public MonthlyReservationResponse getMonthly(YearMonth month, ReservationStatus status) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        List<Reservation> reservations = reservationRepository.findMonthly(start, end, status);

        Map<ReservationStatus, Long> grouped = reservations.stream()
            .collect(Collectors.groupingBy(
                Reservation::getStatus,
                () -> new EnumMap<>(ReservationStatus.class),
                Collectors.counting()
            ));
        for (ReservationStatus value : ReservationStatus.values()) {
            grouped.putIfAbsent(value, 0L);
        }
        return new MonthlyReservationResponse(
            month, grouped, reservations.stream().map(ReservationResponse::from).toList()
        );
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("End time must be after start time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Start time must be in the future");
        }
    }
}
