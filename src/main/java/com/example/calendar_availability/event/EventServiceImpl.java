package com.example.calendar_availability.event;

import com.example.calendar_availability.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        validateTimeRange(request.getStartAt(), request.getEndAt());

        Event event = mapToEntity(request);
        checkOverlap(event, null);

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    @Override
    public List<EventResponse> getEvents(Long ownerId, ZonedDateTime from, ZonedDateTime to) {
        validateOwnerExists(ownerId);
        validateTimeRange(from, to);

        return eventRepository
                .findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(ownerId, to, from)
                .stream()
                .map(this::mapToResponse)
                .toList();
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

    // ======================== Mapping ========================

    private Event mapToEntity(CreateEventRequest request) {
        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setTimezone(request.getTimezone());
        event.setType(request.getType());
        event.setOwnerId(request.getOwnerId());
        event.setNotes(request.getNotes());
        event.setLocation(request.getLocation());
        event.setAttendees(request.getAttendees());
        return event;
    }

    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .timezone(event.getTimezone())
                .type(event.getType())
                .ownerId(event.getOwnerId())
                .notes(event.getNotes())
                .location(event.getLocation())
                .attendees(event.getAttendees())
                .build();
    }

    // ======================== Validation ========================

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