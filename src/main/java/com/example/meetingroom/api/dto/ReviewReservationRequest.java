package com.example.meetingroom.api.dto;

import com.example.meetingroom.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReservationRequest(
    @NotNull Long reviewerId,
    @NotNull ReservationStatus status,
    @Size(max = 500) String reason
) {
}
