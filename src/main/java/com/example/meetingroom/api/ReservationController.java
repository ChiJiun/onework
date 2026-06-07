package com.example.meetingroom.api;

import com.example.meetingroom.api.dto.CreateReservationRequest;
import com.example.meetingroom.api.dto.MonthlyReservationResponse;
import com.example.meetingroom.api.dto.ReservationResponse;
import com.example.meetingroom.api.dto.ReviewReservationRequest;
import com.example.meetingroom.api.dto.TimelineResponse;
import com.example.meetingroom.domain.ReservationStatus;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.exception.ForbiddenOperationException;
import com.example.meetingroom.security.CurrentUserService;
import com.example.meetingroom.service.MeetingRoomService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final MeetingRoomService meetingRoomService;
    private final CurrentUserService currentUserService;

    public ReservationController(
        MeetingRoomService meetingRoomService,
        CurrentUserService currentUserService
    ) {
        this.meetingRoomService = meetingRoomService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
        @Valid @RequestBody CreateReservationRequest request,
        Authentication authentication
    ) {
        User currentUser = currentUserService.requireUser(authentication);
        if (!currentUser.getId().equals(request.userId())) {
            throw new ForbiddenOperationException(
                "A reservation can only be created for the authenticated user"
            );
        }
        ReservationResponse response = ReservationResponse.from(
            meetingRoomService.createReservation(request)
        );
        return ResponseEntity.created(URI.create("/api/reservations/" + response.id())).body(response);
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ReservationResponse review(
        @PathVariable Long id,
        @Valid @RequestBody ReviewReservationRequest request,
        Authentication authentication
    ) {
        User currentUser = currentUserService.requireUser(authentication);
        if (!currentUser.getId().equals(request.reviewerId())) {
            throw new ForbiddenOperationException("Reviewer ID must match the authenticated user");
        }
        return ReservationResponse.from(meetingRoomService.review(id, request));
    }

    @GetMapping("/timeline")
    public TimelineResponse timeline(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return meetingRoomService.getTimeline(date);
    }

    @GetMapping("/monthly")
    public MonthlyReservationResponse monthly(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        @RequestParam(required = false) ReservationStatus status
    ) {
        return meetingRoomService.getMonthly(month, status);
    }
}
