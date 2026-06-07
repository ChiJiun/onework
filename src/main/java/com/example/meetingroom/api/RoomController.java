package com.example.meetingroom.api;

import com.example.meetingroom.api.dto.ReservationResponse;
import com.example.meetingroom.api.dto.RoomResponse;
import com.example.meetingroom.repository.RoomRepository;
import com.example.meetingroom.service.MeetingRoomService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final MeetingRoomService meetingRoomService;

    public RoomController(RoomRepository roomRepository, MeetingRoomService meetingRoomService) {
        this.roomRepository = roomRepository;
        this.meetingRoomService = meetingRoomService;
    }

    @GetMapping
    public List<RoomResponse> rooms() {
        return roomRepository.findAll().stream().map(RoomResponse::from).toList();
    }

    @GetMapping("/{roomId}/reservations")
    public List<ReservationResponse> reservations(@PathVariable Long roomId) {
        return meetingRoomService.getReservationsByRoomId(roomId).stream()
            .map(ReservationResponse::from)
            .toList();
    }

    @GetMapping("/by-name/{roomName}/reservations")
    public List<ReservationResponse> reservationsByRoomName(@PathVariable String roomName) {
        return meetingRoomService.getReservationsByRoomName(roomName).stream()
            .map(ReservationResponse::from)
            .toList();
    }
}
