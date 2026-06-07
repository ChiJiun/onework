package com.example.meetingroom.config;

import com.example.meetingroom.domain.Room;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.domain.UserRole;
import com.example.meetingroom.repository.RoomRepository;
import com.example.meetingroom.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
        RoomRepository roomRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (roomRepository.count() == 0) {
                roomRepository.save(new Room("Taipei 101", 12, "3F", "TV, whiteboard, video conference"));
                roomRepository.save(new Room("Sun Moon Lake", 6, "3F", "TV, whiteboard"));
                roomRepository.save(new Room("Jade Mountain", 20, "5F", "Projector, microphone"));
            }
            if (userRepository.count() == 0) {
                userRepository.save(new User(
                    "alice", "alice@example.com", "Alice Chen",
                    passwordEncoder.encode("password"), UserRole.USER
                ));
                userRepository.save(new User(
                    "reviewer", "reviewer@example.com", "Review Manager",
                    passwordEncoder.encode("password"), UserRole.REVIEWER
                ));
                userRepository.save(new User(
                    "admin", "admin@example.com", "System Admin",
                    passwordEncoder.encode("password"), UserRole.ADMIN
                ));
            }
        };
    }
}
