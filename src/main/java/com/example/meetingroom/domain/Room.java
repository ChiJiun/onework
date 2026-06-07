package com.example.meetingroom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 200)
    private String location;

    @Column(length = 500)
    private String equipment;

    @Column(nullable = false)
    private boolean active = true;

    protected Room() {
    }

    public Room(String name, Integer capacity, String location, String equipment) {
        this.name = name;
        this.capacity = capacity;
        this.location = location;
        this.equipment = equipment;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getLocation() {
        return location;
    }

    public String getEquipment() {
        return equipment;
    }

    public boolean isActive() {
        return active;
    }
}
