package com.example.calendar_availability.event;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@Validated @RequestBody CreateEventRequest request) {
        return new ResponseEntity<>(eventService.createEvent(request), HttpStatus.CREATED);
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam Long ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to) {
        return ResponseEntity.ok(eventService.getEvents(ownerId, from, to));
    }

    @PostMapping("/availability/query")
    public ResponseEntity<List<TimeSlot>> queryAvailability(
            @Validated @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(eventService.getAvailability(request));
    }

}