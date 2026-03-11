package com.example.calendar_availability.event;

import com.example.calendar_availability.exception.CustomException;
import com.example.calendar_availability.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZonedDateTime TODAY_9AM  = ZonedDateTime.of(2026, 3, 11, 9, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_10AM = ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_11AM = ZonedDateTime.of(2026, 3, 11, 11, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_5PM  = ZonedDateTime.of(2026, 3, 11, 17, 0, 0, 0, ZONE);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EventResponse buildEventResponse(Long id, String title, ZonedDateTime start, ZonedDateTime end,
                                             EventType type, Long ownerId) {
        return EventResponse.builder()
                .id(id)
                .title(title)
                .startAt(start)
                .endAt(end)
                .timezone("Asia/Ho_Chi_Minh")
                .type(type)
                .ownerId(ownerId)
                .build();
    }

    /** Helper: build JSON cho Event request body */
    private String eventJson(String title, String startAt, String endAt,
                             String timezone, String type, Long ownerId) {
        return """
                {
                  "title": %s,
                  "startAt": %s,
                  "endAt": %s,
                  "timezone": %s,
                  "type": %s,
                  "ownerId": %s
                }
                """.formatted(
                jsonVal(title), jsonVal(startAt), jsonVal(endAt),
                jsonVal(timezone), jsonVal(type),
                ownerId != null ? ownerId.toString() : "null"
        );
    }

    /** Helper: build JSON cho AvailabilityRequest body */
    private String availabilityJson(Long ownerId, String from, String to) {
        return """
                {
                  "ownerId": %s,
                  "from": "%s",
                  "to": "%s"
                }
                """.formatted(ownerId, from, to);
    }

    private String jsonVal(String val) {
        return val != null ? "\"" + val + "\"" : "null";
    }

    // ========================================================================
    // 1. VALIDATION TESTS – Missing fields via API → 400
    // ========================================================================
    @Nested
    @DisplayName("POST /api/events – Validation")
    class CreateEventValidationTests {

        @Test
        @DisplayName("Valid event → 201 CREATED")
        void createEvent_valid_returns201() throws Exception {
            EventResponse saved = buildEventResponse(1L, "Meeting", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 1L);
            when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(saved);

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Meeting",
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T10:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", 1L)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Meeting"));
        }

        @Test
        @DisplayName("Missing title → 400 BAD REQUEST")
        void createEvent_missingTitle_returns400() throws Exception {
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson(null,
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T10:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", 1L)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Blank title → 400 BAD REQUEST")
        void createEvent_blankTitle_returns400() throws Exception {
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("",
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T10:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", 1L)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing startAt → 400 BAD REQUEST")
        void createEvent_missingStartAt_returns400() throws Exception {
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Test",
                                    null, "2026-03-11T10:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", 1L)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing ownerId → 400 BAD REQUEST")
        void createEvent_missingOwnerId_returns400() throws Exception {
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Test",
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T10:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", null)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing timezone → 400 BAD REQUEST")
        void createEvent_missingTimezone_returns400() throws Exception {
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Test",
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T10:00:00+07:00",
                                    null, "APPOINTMENT", 1L)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing event type → 400 BAD REQUEST")
        void createEvent_missingType_returns400() throws Exception {
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Test",
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T10:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", null, 1L)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // 3. COMPLICATION TESTS – Overlap rejection via API → 409
    // ========================================================================
    @Nested
    @DisplayName("POST /api/events – Overlap Rejection")
    class OverlapRejectionViaApiTests {

        @Test
        @DisplayName("Overlap detected → 409 CONFLICT with error message")
        void createEvent_overlap_returns409() throws Exception {
            when(eventService.createEvent(any(CreateEventRequest.class)))
                    .thenThrow(new CustomException(
                            "Appointment overlaps with an existing appointment", HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Overlap",
                                    "2026-03-11T09:00:00+07:00", "2026-03-11T11:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", 1L)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message", containsString("overlaps")));
        }

        @Test
        @DisplayName("Invalid time range → 400 BAD REQUEST via service")
        void createEvent_invalidRange_returns400() throws Exception {
            when(eventService.createEvent(any(CreateEventRequest.class)))
                    .thenThrow(new CustomException(
                            "Start time must be strictly before end time", HttpStatus.BAD_REQUEST));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("Bad",
                                    "2026-03-11T11:00:00+07:00", "2026-03-11T09:00:00+07:00",
                                    "Asia/Ho_Chi_Minh", "APPOINTMENT", 1L)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // ========================================================================
    // GET /api/events – Range query
    // ========================================================================
    @Nested
    @DisplayName("GET /api/events")
    class GetEventsTests {

        @Test
        @DisplayName("Valid query → 200 OK with events")
        void getEvents_valid_returns200() throws Exception {
            EventResponse event = buildEventResponse(1L, "Meeting", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 1L);
            when(eventService.getEvents(eq(1L), any(), any())).thenReturn(List.of(event));

            mockMvc.perform(get("/api/events")
                            .param("ownerId", "1")
                            .param("from", TODAY_9AM.toString())
                            .param("to", TODAY_5PM.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Meeting"));
        }

        @Test
        @DisplayName("No events in range → 200 OK with empty list")
        void getEvents_noEvents_returnsEmptyList() throws Exception {
            when(eventService.getEvents(eq(1L), any(), any())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/events")
                            .param("ownerId", "1")
                            .param("from", TODAY_9AM.toString())
                            .param("to", TODAY_5PM.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Non-existent ownerId → 404 NOT FOUND")
        void getEvents_ownerNotFound_returns404() throws Exception {
            when(eventService.getEvents(eq(999L), any(), any()))
                    .thenThrow(new CustomException("Owner with id 999 not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/events")
                            .param("ownerId", "999")
                            .param("from", TODAY_9AM.toString())
                            .param("to", TODAY_5PM.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message", containsString("Owner with id 999 not found")));
        }
    }

    // ========================================================================
    // POST /api/availability/query – Availability
    // ========================================================================
    @Nested
    @DisplayName("POST /api/availability/query")
    class AvailabilityQueryTests {

        @Test
        @DisplayName("Valid query → 200 OK with available slots")
        void queryAvailability_valid_returns200() throws Exception {
            TimeSlot slot1 = new TimeSlot(TODAY_9AM, TODAY_10AM);
            TimeSlot slot2 = new TimeSlot(TODAY_11AM, TODAY_5PM);
            when(eventService.getAvailability(any(AvailabilityRequest.class)))
                    .thenReturn(List.of(slot1, slot2));

            mockMvc.perform(post("/api/availability/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(availabilityJson(1L,
                                    "2026-03-11T09:00:00+07:00",
                                    "2026-03-11T17:00:00+07:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Fully booked → 200 OK with empty list")
        void queryAvailability_fullyBooked_returnsEmptyList() throws Exception {
            when(eventService.getAvailability(any(AvailabilityRequest.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/availability/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(availabilityJson(1L,
                                    "2026-03-11T09:00:00+07:00",
                                    "2026-03-11T17:00:00+07:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Non-existent ownerId → 404 NOT FOUND")
        void queryAvailability_ownerNotFound_returns404() throws Exception {
            when(eventService.getAvailability(any(AvailabilityRequest.class)))
                    .thenThrow(new CustomException("Owner with id 999 not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(post("/api/availability/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(availabilityJson(999L,
                                    "2026-03-11T09:00:00+07:00",
                                    "2026-03-11T17:00:00+07:00")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message", containsString("Owner with id 999 not found")));
        }
    }
}

