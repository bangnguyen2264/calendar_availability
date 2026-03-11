package com.example.calendar_availability.event;
import com.example.calendar_availability.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service // Vẫn giữ annotation này ở lớp Implementation
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    @Transactional
    public Event createEvent(Event event) {
        validateTimeRange(event.getStartAt(), event.getEndAt());
        checkOverlap(event, null);
        return eventRepository.save(event);
    }

    @Override
    public List<Event> getEvents(Long ownerId, ZonedDateTime from, ZonedDateTime to) {
        validateOwnerExists(ownerId);
        validateTimeRange(from, to);

         return eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(ownerId, to, from);
    }

    @Override
    public List<TimeSlot> getAvailability(AvailabilityRequest request) {
        validateOwnerExists(request.getOwnerId());
        validateTimeRange(request.getFrom(), request.getTo());

        List<Event> events = eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(
                request.getOwnerId(), request.getTo(), request.getFrom());

        List<TimeSlot> availableSlots = new ArrayList<>();
        ZonedDateTime currentStart = request.getFrom();

        for (Event event : events) {
            if (currentStart.isBefore(event.getStartAt())) {
                availableSlots.add(new TimeSlot(currentStart, event.getStartAt()));
            }
            if (currentStart.isBefore(event.getEndAt())) {
                currentStart = event.getEndAt();
            }
        }

        if (currentStart.isBefore(request.getTo())) {
            availableSlots.add(new TimeSlot(currentStart, request.getTo()));
        }

        return availableSlots;
    }

    // Các hàm helper (private) giữ nguyên, không cần đưa vào Interface
    private void validateOwnerExists(Long ownerId) {
        if (!eventRepository.existsByOwnerId(ownerId)) {
            throw new CustomException("Owner with id " + ownerId + " not found", HttpStatus.NOT_FOUND);
        }
    }

    private void validateTimeRange(ZonedDateTime start, ZonedDateTime end) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new CustomException("Start time must be strictly before end time", HttpStatus.BAD_REQUEST);
        }
    }

    private void checkOverlap(Event event, Long excludeId) {
        if (event.getType() == EventType.APPOINTMENT) {
            boolean overlaps = eventRepository.isOverlapping(
                    event.getOwnerId(),
                    EventType.APPOINTMENT,
                    event.getStartAt(),
                    event.getEndAt(),
                    excludeId
            );
            if (overlaps) {
                throw new CustomException("Appointment overlaps with an existing appointment", HttpStatus.CONFLICT);
            }
        }
    }
}