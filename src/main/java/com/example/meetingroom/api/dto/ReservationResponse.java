package com.example.meetingroom.api.dto;

import com.example.meetingroom.domain.Reservation;
import com.example.meetingroom.domain.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationResponse(
    Long id,
    Long roomId,
    String roomName,
    Long requesterId,
    String requesterName,
    Long reviewerId,
    String reviewerName,
    String title,
    String description,
    Integer attendeeCount,
    LocalDateTime startTime,
    LocalDateTime endTime,
    ReservationStatus status,
    String rejectionReason,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoom().getId(),
            reservation.getRoom().getName(),
            reservation.getRequester().getId(),
            reservation.getRequester().getDisplayName(),
            reservation.getReviewer() == null ? null : reservation.getReviewer().getId(),
            reservation.getReviewer() == null ? null : reservation.getReviewer().getDisplayName(),
            reservation.getTitle(),
            reservation.getDescription(),
            reservation.getAttendeeCount(),
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getStatus(),
            reservation.getRejectionReason(),
            reservation.getReviewedAt(),
            reservation.getCreatedAt()
        );
    }
}
