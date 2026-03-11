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
    public ResponseEntity<Event> createEvent(@Validated @RequestBody Event event) {
        return new ResponseEntity<>(eventService.createEvent(event), HttpStatus.CREATED);
    }
   @GetMapping("/events")
    public ResponseEntity<List<Event>> getEvents(
            @RequestParam Long ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to) {
        return ResponseEntity.ok(eventService.getEvents(ownerId, from, to));
    }

      @PostMapping("/availability/query")
    public ResponseEntity<List<TimeSlot>> queryAvailability(@Validated @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(eventService.getAvailability(request));
    }

}