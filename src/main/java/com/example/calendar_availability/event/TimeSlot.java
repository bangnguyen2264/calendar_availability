package com.example.calendar_availability.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class TimeSlot {
    private ZonedDateTime startAt;
    private ZonedDateTime endAt;
}
