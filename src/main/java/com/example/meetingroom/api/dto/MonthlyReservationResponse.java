package com.example.meetingroom.api.dto;

import com.example.meetingroom.domain.ReservationStatus;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record MonthlyReservationResponse(
    YearMonth month,
    Map<ReservationStatus, Long> counts,
    List<ReservationResponse> reservations
) {
}
