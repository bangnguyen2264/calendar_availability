package com.example.calendar_availability.event;

import java.time.ZonedDateTime;
import java.util.List;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request);

    List<EventResponse> getEvents(Long ownerId, ZonedDateTime from, ZonedDateTime to);

    List<TimeSlot> getAvailability(AvailabilityRequest request);

}