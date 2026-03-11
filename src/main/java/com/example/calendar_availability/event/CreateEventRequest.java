package com.example.calendar_availability.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Start time is required")
    private ZonedDateTime startAt;

    @NotNull(message = "End time is required")
    private ZonedDateTime endAt;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    @NotNull(message = "Event type is required")
    private EventType type;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    private String notes;
    private String location;
    private String attendees;
}

