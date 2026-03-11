package com.example.calendar_availability.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {
    private Long ownerId;
    private ZonedDateTime from;
    private ZonedDateTime to;
}
