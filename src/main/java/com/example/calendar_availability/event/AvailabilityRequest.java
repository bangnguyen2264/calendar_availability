package com.example.calendar_availability.event;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    @NotNull(message = "From time is required")
    private ZonedDateTime from;

    @NotNull(message = "To time is required")
    private ZonedDateTime to;
}
