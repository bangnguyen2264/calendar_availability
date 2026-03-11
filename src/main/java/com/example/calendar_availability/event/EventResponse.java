package com.example.calendar_availability.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private ZonedDateTime startAt;
    private ZonedDateTime endAt;
    private String timezone;
    private EventType type;
    private Long ownerId;
    private String notes;
    private String location;
    private String attendees;
}

