package com.example.meetingroom;

import com.example.meetingroom.config.AdminLoginProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AdminLoginProperties.class)
public class MeetingRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingRoomApplication.class, args);
    }
}
