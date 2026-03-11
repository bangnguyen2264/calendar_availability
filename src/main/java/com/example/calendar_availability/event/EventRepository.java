package com.example.calendar_availability.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Kiểm tra ownerId có tồn tại trong hệ thống không
    boolean existsByOwnerId(Long ownerId);

    // Thay @Query("... e.ownerId = :ownerId AND e.startAt < :to AND e.endAt > :from ORDER BY e.startAt ASC")
    List<Event> findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(
            Long ownerId, ZonedDateTime to, ZonedDateTime from);

    // Kiểm tra trùng lịch (KHÔNG loại trừ event nào)
    boolean existsByOwnerIdAndTypeAndStartAtBeforeAndEndAtAfter(
            Long ownerId, EventType type, ZonedDateTime endAt, ZonedDateTime startAt);

    // Kiểm tra trùng lịch (loại trừ 1 event theo id)
    boolean existsByOwnerIdAndTypeAndIdNotAndStartAtBeforeAndEndAtAfter(
            Long ownerId, EventType type, Long id, ZonedDateTime endAt, ZonedDateTime startAt);

    // Gộp 2 method trên, thay thế @Query isOverlapping cũ
    default boolean isOverlapping(Long ownerId, EventType type,
                                  ZonedDateTime startAt, ZonedDateTime endAt,
                                  Long excludeId) {
        if (excludeId == null) {
            return existsByOwnerIdAndTypeAndStartAtBeforeAndEndAtAfter(
                    ownerId, type, endAt, startAt);
        }
        return existsByOwnerIdAndTypeAndIdNotAndStartAtBeforeAndEndAtAfter(
                ownerId, type, excludeId, endAt, startAt);
    }
}