package com.example.meetingroom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "attendee_count", nullable = false)
    private Integer attendeeCount;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PROCESSING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    protected Reservation() {
    }

    public Reservation(Room room, User requester, String title, String description,
                       Integer attendeeCount, LocalDateTime startTime, LocalDateTime endTime) {
        this.room = room;
        this.requester = requester;
        this.title = title;
        this.description = description;
        this.attendeeCount = attendeeCount;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void approve(User reviewer) {
        requireProcessing();
        this.status = ReservationStatus.APPROVED;
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(User reviewer, String reason) {
        requireProcessing();
        this.status = ReservationStatus.REJECTED;
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    private void requireProcessing() {
        if (status != ReservationStatus.PROCESSING) {
            throw new IllegalStateException("Only processing reservations can be reviewed");
        }
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getRequester() {
        return requester;
    }

    public User getReviewer() {
        return reviewer;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getAttendeeCount() {
        return attendeeCount;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
