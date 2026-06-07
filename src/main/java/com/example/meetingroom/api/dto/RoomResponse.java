package com.example.meetingroom.api.dto;

import com.example.meetingroom.domain.Room;

public record RoomResponse(
    Long id,
    String name,
    Integer capacity,
    String location,
    String equipment,
    boolean active
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(),
            room.getLocation(), room.getEquipment(), room.isActive());
    }
}
