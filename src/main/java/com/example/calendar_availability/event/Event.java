package com.example.calendar_availability.event;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank(message = "Title is required")
    private String title; // Event title [cite: 9]

    @NotNull(message = "Start time is required")
    private ZonedDateTime startAt;

    @NotNull(message = "End time is required")
    private ZonedDateTime endAt;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    private EventType type;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;
    private String notes;
    private String location;
    private String attendees;
}


