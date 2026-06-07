package com.example.meetingroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.meetingroom.api.dto.CreateReservationRequest;
import com.example.meetingroom.api.dto.MonthlyReservationResponse;
import com.example.meetingroom.api.dto.ReviewReservationRequest;
import com.example.meetingroom.api.dto.TimelineResponse;
import com.example.meetingroom.domain.Reservation;
import com.example.meetingroom.domain.ReservationStatus;
import com.example.meetingroom.domain.Room;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.domain.UserRole;
import com.example.meetingroom.exception.BusinessRuleException;
import com.example.meetingroom.exception.ReservationConflictException;
import com.example.meetingroom.repository.RoomRepository;
import com.example.meetingroom.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MeetingRoomServiceIntegrationTest {

    @Autowired
    private MeetingRoomService meetingRoomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private Room room;
    private User requester;
    private User reviewer;

    @BeforeEach
    void setUp() {
        room = roomRepository.findAll().get(0);
        requester = userRepository.findByUsername("alice").orElseThrow();
        reviewer = userRepository.findByUsername("reviewer").orElseThrow();
    }

    @Test
    void createsProcessingReservationAndRejectsOverlap() {
        LocalDateTime start = LocalDateTime.now().plusDays(10).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        Reservation created = meetingRoomService.createReservation(request(start, end, 4));

        assertThat(created.getStatus()).isEqualTo(ReservationStatus.PROCESSING);
        assertThatThrownBy(() ->
            meetingRoomService.createReservation(request(start.plusMinutes(30), end.plusMinutes(30), 2))
        ).isInstanceOf(ReservationConflictException.class);
    }

    @Test
    void allowsAdjacentReservations() {
        LocalDateTime start = LocalDateTime.now().plusDays(11).withSecond(0).withNano(0);
        meetingRoomService.createReservation(request(start, start.plusHours(1), 2));

        Reservation adjacent = meetingRoomService.createReservation(
            request(start.plusHours(1), start.plusHours(2), 2)
        );

        assertThat(adjacent.getId()).isNotNull();
    }

    @Test
    void validatesCapacityAndTimeRange() {
        LocalDateTime start = LocalDateTime.now().plusDays(12).withSecond(0).withNano(0);

        assertThatThrownBy(() ->
            meetingRoomService.createReservation(request(start, start.plusHours(1), room.getCapacity() + 1))
        ).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("capacity");

        assertThatThrownBy(() ->
            meetingRoomService.createReservation(request(start, start, 1))
        ).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("End time");
    }

    @Test
    void reviewTimelineAndMonthlyReportFollowStatusRules() {
        LocalDateTime start = LocalDateTime.now().plusMonths(2).withDayOfMonth(5)
            .withHour(10).withMinute(0).withSecond(0).withNano(0);
        Reservation reservation = meetingRoomService.createReservation(request(start, start.plusHours(1), 3));

        Reservation approved = meetingRoomService.review(
            reservation.getId(),
            new ReviewReservationRequest(reviewer.getId(), ReservationStatus.APPROVED, null)
        );
        TimelineResponse timeline = meetingRoomService.getTimeline(start.toLocalDate());
        MonthlyReservationResponse monthly =
            meetingRoomService.getMonthly(YearMonth.from(start), ReservationStatus.APPROVED);

        assertThat(approved.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        assertThat(timeline.reservedRoomCount()).isEqualTo(1);
        assertThat(timeline.reservations()).extracting("id").contains(reservation.getId());
        assertThat(monthly.counts().get(ReservationStatus.APPROVED)).isEqualTo(1);
    }

    @Test
    void rejectedReservationReleasesTimeSlot() {
        LocalDateTime start = LocalDateTime.now().plusMonths(3).withDayOfMonth(8)
            .withHour(9).withMinute(0).withSecond(0).withNano(0);
        Reservation rejected = meetingRoomService.createReservation(request(start, start.plusHours(1), 2));

        meetingRoomService.review(
            rejected.getId(),
            new ReviewReservationRequest(reviewer.getId(), ReservationStatus.REJECTED, "Schedule conflict")
        );
        Reservation replacement =
            meetingRoomService.createReservation(request(start, start.plusHours(1), 2));

        assertThat(replacement.getStatus()).isEqualTo(ReservationStatus.PROCESSING);
    }

    @Test
    void rejectsInvalidReviewerAndRepeatedReview() {
        LocalDateTime start = LocalDateTime.now().plusMonths(4).withDayOfMonth(10)
            .withHour(14).withMinute(0).withSecond(0).withNano(0);
        Reservation reservation = meetingRoomService.createReservation(request(start, start.plusHours(1), 2));

        assertThat(requester.getRole()).isEqualTo(UserRole.USER);
        assertThatThrownBy(() -> meetingRoomService.review(
            reservation.getId(),
            new ReviewReservationRequest(requester.getId(), ReservationStatus.APPROVED, null)
        )).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("not allowed");

        meetingRoomService.review(
            reservation.getId(),
            new ReviewReservationRequest(reviewer.getId(), ReservationStatus.APPROVED, null)
        );
        assertThatThrownBy(() -> meetingRoomService.review(
            reservation.getId(),
            new ReviewReservationRequest(reviewer.getId(), ReservationStatus.REJECTED, "Changed mind")
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("processing");
    }

    private CreateReservationRequest request(
        LocalDateTime start, LocalDateTime end, int attendeeCount
    ) {
        return new CreateReservationRequest(
            room.getId(),
            requester.getId(),
            "Project sync",
            "Weekly project sync",
            attendeeCount,
            start,
            end
        );
    }
}
