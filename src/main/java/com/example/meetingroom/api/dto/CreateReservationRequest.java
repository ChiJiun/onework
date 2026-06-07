package com.example.meetingroom.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateReservationRequest(
    @NotNull Long roomId,
    @NotNull Long userId,
    @NotBlank @Size(max = 150) String title,
    @Size(max = 1000) String description,
    @NotNull @Min(1) Integer attendeeCount,
    @NotNull @Future LocalDateTime startTime,
    @NotNull @Future LocalDateTime endTime
) {
}
