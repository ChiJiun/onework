package com.example.meetingroom;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetingroom.api.dto.CreateReservationRequest;
import com.example.meetingroom.domain.Room;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.repository.ReservationRepository;
import com.example.meetingroom.repository.RoomRepository;
import com.example.meetingroom.repository.UserRepository;
import com.example.meetingroom.service.MeetingRoomService;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MeetingRoomPostgresContainerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MeetingRoomService service;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void concurrentRequestsOnlyCreateOneReservation() throws Exception {
        Room room = roomRepository.findAll().get(0);
        User user = userRepository.findByUsername("alice").orElseThrow();
        LocalDateTime start = LocalDateTime.now().plusYears(1).withNano(0);
        CreateReservationRequest request = new CreateReservationRequest(
            room.getId(), user.getId(), "Concurrent booking", null, 2, start, start.plusHours(1)
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> reserve(ready, go, request));
            Future<Boolean> second = pool.submit(() -> reserve(ready, go, request));
            ready.await();
            go.countDown();

            assertThat(first.get() ^ second.get()).isTrue();
            assertThat(reservationRepository.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean reserve(
        CountDownLatch ready, CountDownLatch go, CreateReservationRequest request
    ) throws InterruptedException {
        ready.countDown();
        go.await();
        try {
            service.createReservation(request);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
