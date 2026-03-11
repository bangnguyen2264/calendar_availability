package com.example.calendar_availability.config;

import com.example.calendar_availability.event.Event;
import com.example.calendar_availability.event.EventType;
import com.example.calendar_availability.event.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public CommandLineRunner initDatabase(EventRepository eventRepository) {
        return args -> {
            if (eventRepository.count() == 0) {
                ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
                ZonedDateTime today = ZonedDateTime.now(zoneId).withHour(0).withMinute(0).withSecond(0).withNano(0);

                Event e1 = buildEvent("Họp Daily Scrum", today.withHour(9), today.withHour(9).plusMinutes(30), EventType.APPOINTMENT, 1L, "Cập nhật tiến độ", "Google Meet", "team@masterbranch.com");
                Event e2 = buildEvent("Phỏng vấn Backend MasterBranch", today.withHour(10), today.withHour(11).plusMinutes(30), EventType.APPOINTMENT, 1L, "Review source code", "Phòng họp 1", "luuduchieuharry@gmail.com, kimchi.masterbranch@gmail.com, diemsuong.masterbranch@gmail.com");
                Event e3 = buildEvent("Nghỉ trưa", today.withHour(12), today.withHour(13).plusMinutes(30), EventType.BLOCK, 1L, "Không nhận lịch hẹn", null, null);
                Event e4 = buildEvent("Họp chốt UI/UX", today.withHour(14), today.withHour(15), EventType.APPOINTMENT, 1L, null, "Phòng họp 2", "design@masterbranch.com");

                Event e5 = buildEvent("Làm việc với đối tác", today.withHour(10), today.withHour(12), EventType.APPOINTMENT, 2L, "Ký hợp đồng", "Quán Cafe", "client@test.com");

                ZonedDateTime tomorrow = today.plusDays(1);
                Event e6 = buildEvent("Code Review", tomorrow.withHour(9), tomorrow.withHour(11), EventType.APPOINTMENT, 1L, "Review module Availability", "Online", "dev@masterbranch.com");
                Event e7 = buildEvent("Đi khám sức khỏe", tomorrow.withHour(13), tomorrow.withHour(16), EventType.BLOCK, 1L, "Nghỉ phép nửa ngày", "Bệnh viện", null);

                Event e8 = buildEvent("Phỏng vấn ứng viên Frontend", tomorrow.withHour(9), tomorrow.withHour(10), EventType.APPOINTMENT, 3L, "Vòng 1", "Google Meet", "hr@test.com");

                ZonedDateTime dayAfterTomorrow = today.plusDays(2);
                Event e9 = buildEvent("Triển khai hệ thống (Deploy)", dayAfterTomorrow.withHour(2), dayAfterTomorrow.withHour(4), EventType.BLOCK, 1L, "Bảo trì server", "Server Room", null);
                Event e10 = buildEvent("Báo cáo tiến độ tuần", dayAfterTomorrow.withHour(15), dayAfterTomorrow.withHour(16).plusMinutes(30), EventType.APPOINTMENT, 1L, "Chuẩn bị slide", "Phòng họp Lớn", "all_hands@masterbranch.com");

                eventRepository.saveAll(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
                System.out.println("Đã khởi tạo 10 sự kiện demo thành công!");
            } else {
                System.out.println("Dữ liệu đã tồn tại, bỏ qua bước khởi tạo.");
            }
        };
    }

    private Event buildEvent(String title, ZonedDateTime start, ZonedDateTime end,
                             EventType type, Long ownerId, String notes, String location, String attendees) {
        Event event = new Event();
        event.setTitle(title); //
        event.setStartAt(start); //
        event.setEndAt(end); //
        event.setTimezone("Asia/Ho_Chi_Minh"); //
        event.setType(type); //
        event.setOwnerId(ownerId); //
        event.setNotes(notes); //
        event.setLocation(location); //
        event.setAttendees(attendees); //
        return event;
    }
}