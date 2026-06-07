package com.example.meetingroom.api.dto;

import java.time.LocalDate;
import java.util.List;

public record TimelineResponse(
    LocalDate date,
    long reservedRoomCount,
    List<ReservationResponse> reservations
) {
}
