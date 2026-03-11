package com.example.calendar_availability.event;

import com.example.calendar_availability.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZonedDateTime TODAY_9AM  = ZonedDateTime.of(2026, 3, 11, 9, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_10AM = ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_11AM = ZonedDateTime.of(2026, 3, 11, 11, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_12PM = ZonedDateTime.of(2026, 3, 11, 12, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_1PM  = ZonedDateTime.of(2026, 3, 11, 13, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_2PM  = ZonedDateTime.of(2026, 3, 11, 14, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_3PM  = ZonedDateTime.of(2026, 3, 11, 15, 0, 0, 0, ZONE);
    private static final ZonedDateTime TODAY_5PM  = ZonedDateTime.of(2026, 3, 11, 17, 0, 0, 0, ZONE);

    private Event buildEvent(Long id, String title, ZonedDateTime start, ZonedDateTime end,
                             EventType type, Long ownerId) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setStartAt(start);
        event.setEndAt(end);
        event.setTimezone("Asia/Ho_Chi_Minh");
        event.setType(type);
        event.setOwnerId(ownerId);
        return event;
    }

    // ========================================================================
    // 1. VALIDATION TESTS – Missing fields or invalid time ranges
    // ========================================================================
    @Nested
    @DisplayName("1. Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("createEvent – startAt equals endAt → BAD_REQUEST")
        void createEvent_startEqualsEnd_shouldThrow() {
            Event event = buildEvent(null, "Meeting", TODAY_9AM, TODAY_9AM, EventType.APPOINTMENT, 1L);

            assertThatThrownBy(() -> eventService.createEvent(event))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Start time must be strictly before end time")
                    .satisfies(ex -> assertThat(((CustomException) ex).getStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("createEvent – startAt after endAt → BAD_REQUEST")
        void createEvent_startAfterEnd_shouldThrow() {
            Event event = buildEvent(null, "Meeting", TODAY_11AM, TODAY_9AM, EventType.APPOINTMENT, 1L);

            assertThatThrownBy(() -> eventService.createEvent(event))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Start time must be strictly before end time");
        }

        @Test
        @DisplayName("getEvents – invalid range (from > to) → BAD_REQUEST")
        void getEvents_invalidRange_shouldThrow() {
            when(eventRepository.existsByOwnerId(1L)).thenReturn(true);

            assertThatThrownBy(() -> eventService.getEvents(1L, TODAY_5PM, TODAY_9AM))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Start time must be strictly before end time");
        }

        @Test
        @DisplayName("getEvents – non-existent ownerId → NOT_FOUND")
        void getEvents_ownerNotFound_shouldThrow() {
            when(eventRepository.existsByOwnerId(999L)).thenReturn(false);

            assertThatThrownBy(() -> eventService.getEvents(999L, TODAY_9AM, TODAY_5PM))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Owner with id 999 not found")
                    .satisfies(ex -> assertThat(((CustomException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("getAvailability – invalid range (from == to) → BAD_REQUEST")
        void getAvailability_sameFromTo_shouldThrow() {
            when(eventRepository.existsByOwnerId(1L)).thenReturn(true);
            AvailabilityRequest request = new AvailabilityRequest(1L, TODAY_9AM, TODAY_9AM);

            assertThatThrownBy(() -> eventService.getAvailability(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Start time must be strictly before end time");
        }

        @Test
        @DisplayName("getAvailability – invalid range (from > to) → BAD_REQUEST")
        void getAvailability_fromAfterTo_shouldThrow() {
            when(eventRepository.existsByOwnerId(1L)).thenReturn(true);
            AvailabilityRequest request = new AvailabilityRequest(1L, TODAY_5PM, TODAY_9AM);

            assertThatThrownBy(() -> eventService.getAvailability(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Start time must be strictly before end time");
        }

        @Test
        @DisplayName("getAvailability – non-existent ownerId → NOT_FOUND")
        void getAvailability_ownerNotFound_shouldThrow() {
            when(eventRepository.existsByOwnerId(999L)).thenReturn(false);
            AvailabilityRequest request = new AvailabilityRequest(999L, TODAY_9AM, TODAY_5PM);

            assertThatThrownBy(() -> eventService.getAvailability(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Owner with id 999 not found")
                    .satisfies(ex -> assertThat(((CustomException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ========================================================================
    // 2. RANGE OVERLAP LOGIC – Events partially or fully overlapping query range
    // ========================================================================
    @Nested
    @DisplayName("2. Range Overlap Logic Tests")
    class RangeOverlapLogicTests {

        @BeforeEach
        void setUp() {
            when(eventRepository.existsByOwnerId(1L)).thenReturn(true);
        }

        @Test
        @DisplayName("Event fully within query range → returned")
        void getEvents_fullyWithin() {
            Event event = buildEvent(1L, "Team Sync", TODAY_10AM, TODAY_11AM, EventType.APPOINTMENT, 1L);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(event));

            List<Event> result = eventService.getEvents(1L, TODAY_9AM, TODAY_5PM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getTitle()).isEqualTo("Team Sync");
        }

        @Test
        @DisplayName("Event starts before range (partial overlap) → returned")
        void getEvents_startsBeforeRange() {
            ZonedDateTime today8AM = ZonedDateTime.of(2026, 3, 11, 8, 0, 0, 0, ZONE);
            Event event = buildEvent(1L, "Early", today8AM, TODAY_10AM, EventType.APPOINTMENT, 1L);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(event));

            List<Event> result = eventService.getEvents(1L, TODAY_9AM, TODAY_5PM);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Event ends after range (partial overlap) → returned")
        void getEvents_endsAfterRange() {
            ZonedDateTime today6PM = ZonedDateTime.of(2026, 3, 11, 18, 0, 0, 0, ZONE);
            Event event = buildEvent(1L, "Late", TODAY_3PM, today6PM, EventType.APPOINTMENT, 1L);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(event));

            List<Event> result = eventService.getEvents(1L, TODAY_9AM, TODAY_5PM);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("No events in range → empty list")
        void getEvents_noEvents() {
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(Collections.emptyList());

            List<Event> result = eventService.getEvents(1L, TODAY_9AM, TODAY_5PM);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Multiple events overlapping range → all returned")
        void getEvents_multipleOverlap() {
            Event e1 = buildEvent(1L, "A", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 1L);
            Event e2 = buildEvent(2L, "B", TODAY_11AM, TODAY_12PM, EventType.APPOINTMENT, 1L);
            Event e3 = buildEvent(3L, "C", TODAY_12PM, TODAY_1PM, EventType.BLOCK, 1L);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(e1, e2, e3));

            List<Event> result = eventService.getEvents(1L, TODAY_9AM, TODAY_5PM);

            assertThat(result).hasSize(3);
        }
    }

    // ========================================================================
    // 3. COMPLICATION TESTS – Overlap rejection (HTTP 409 Conflict)
    // ========================================================================
    @Nested
    @DisplayName("3. Overlap Rejection Tests")
    class OverlapRejectionTests {

        @Test
        @DisplayName("APPOINTMENT overlaps existing APPOINTMENT → CONFLICT (409)")
        void createAppointment_overlap_shouldThrow409() {
            Event newEvent = buildEvent(null, "Overlap", TODAY_9AM, TODAY_11AM, EventType.APPOINTMENT, 1L);
            when(eventRepository.isOverlapping(1L, EventType.APPOINTMENT, TODAY_9AM, TODAY_11AM, null))
                    .thenReturn(true);

            assertThatThrownBy(() -> eventService.createEvent(newEvent))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException ce = (CustomException) ex;
                        assertThat(ce.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ce.getMessage()).contains("overlaps");
                    });

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("APPOINTMENT no overlap → saved successfully")
        void createAppointment_noOverlap_shouldSave() {
            Event newEvent = buildEvent(null, "Free Slot", TODAY_2PM, TODAY_3PM, EventType.APPOINTMENT, 1L);
            Event saved = buildEvent(1L, "Free Slot", TODAY_2PM, TODAY_3PM, EventType.APPOINTMENT, 1L);
            when(eventRepository.isOverlapping(1L, EventType.APPOINTMENT, TODAY_2PM, TODAY_3PM, null))
                    .thenReturn(false);
            when(eventRepository.save(newEvent)).thenReturn(saved);

            Event result = eventService.createEvent(newEvent);

            assertThat(result.getId()).isEqualTo(1L);
            verify(eventRepository).save(newEvent);
        }

        @Test
        @DisplayName("BLOCK event does NOT trigger overlap check")
        void createBlock_noOverlapCheck() {
            Event block = buildEvent(null, "Lunch", TODAY_12PM, TODAY_1PM, EventType.BLOCK, 1L);
            Event saved = buildEvent(2L, "Lunch", TODAY_12PM, TODAY_1PM, EventType.BLOCK, 1L);
            when(eventRepository.save(block)).thenReturn(saved);

            Event result = eventService.createEvent(block);

            assertThat(result.getType()).isEqualTo(EventType.BLOCK);
            verify(eventRepository, never()).isOverlapping(anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Partial overlap (new starts during existing) → CONFLICT")
        void createAppointment_partialOverlap_shouldThrow409() {
            Event newEvent = buildEvent(null, "X", TODAY_10AM, TODAY_12PM, EventType.APPOINTMENT, 1L);
            when(eventRepository.isOverlapping(1L, EventType.APPOINTMENT, TODAY_10AM, TODAY_12PM, null))
                    .thenReturn(true);

            assertThatThrownBy(() -> eventService.createEvent(newEvent))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getStatus())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Different owner same time → allowed (no conflict)")
        void createAppointment_differentOwner_shouldSave() {
            Event newEvent = buildEvent(null, "Owner2", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 2L);
            Event saved = buildEvent(3L, "Owner2", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 2L);
            when(eventRepository.isOverlapping(2L, EventType.APPOINTMENT, TODAY_9AM, TODAY_10AM, null))
                    .thenReturn(false);
            when(eventRepository.save(newEvent)).thenReturn(saved);

            Event result = eventService.createEvent(newEvent);

            assertThat(result.getOwnerId()).isEqualTo(2L);
        }
    }

    // ========================================================================
    // 4. AVAILABILITY TESTS – Ensure correct free slots calculation
    // ========================================================================
    @Nested
    @DisplayName("4. Availability Tests")
    class AvailabilityTests {

        @BeforeEach
        void setUp() {
            when(eventRepository.existsByOwnerId(1L)).thenReturn(true);
        }

        @Test
        @DisplayName("No events → full range is available")
        void noEvents_fullRangeAvailable() {
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(Collections.emptyList());

            List<TimeSlot> slots = eventService.getAvailability(req);

            assertThat(slots).hasSize(1);
            assertThat(slots.getFirst().getStartAt()).isEqualTo(TODAY_9AM);
            assertThat(slots.getFirst().getEndAt()).isEqualTo(TODAY_5PM);
        }

        @Test
        @DisplayName("Single event in middle → two free slots (before & after)")
        void singleEvent_twoFreeSlots() {
            Event event = buildEvent(1L, "Meeting", TODAY_10AM, TODAY_11AM, EventType.APPOINTMENT, 1L);
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(event));

            List<TimeSlot> slots = eventService.getAvailability(req);

            assertThat(slots).hasSize(2);
            // 9:00 – 10:00
            assertThat(slots.get(0).getStartAt()).isEqualTo(TODAY_9AM);
            assertThat(slots.get(0).getEndAt()).isEqualTo(TODAY_10AM);
            // 11:00 – 17:00
            assertThat(slots.get(1).getStartAt()).isEqualTo(TODAY_11AM);
            assertThat(slots.get(1).getEndAt()).isEqualTo(TODAY_5PM);
        }

        @Test
        @DisplayName("Fully booked → no free slots")
        void fullyBooked_noSlots() {
            Event e1 = buildEvent(1L, "Morning", TODAY_9AM, TODAY_12PM, EventType.APPOINTMENT, 1L);
            Event e2 = buildEvent(2L, "Afternoon", TODAY_12PM, TODAY_5PM, EventType.BLOCK, 1L);
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(e1, e2));

            List<TimeSlot> slots = eventService.getAvailability(req);

            assertThat(slots).isEmpty();
        }

        @Test
        @DisplayName("Multiple events with gaps → correct free slots")
        void multipleEventsWithGaps() {
            Event e1 = buildEvent(1L, "A", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 1L);
            Event e2 = buildEvent(2L, "B", TODAY_11AM, TODAY_12PM, EventType.APPOINTMENT, 1L);
            Event e3 = buildEvent(3L, "C", TODAY_2PM, TODAY_3PM, EventType.APPOINTMENT, 1L);
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(e1, e2, e3));

            List<TimeSlot> slots = eventService.getAvailability(req);

            // 10–11, 12–14, 15–17
            assertThat(slots).hasSize(3);
            assertThat(slots.get(0)).isEqualTo(new TimeSlot(TODAY_10AM, TODAY_11AM));
            assertThat(slots.get(1)).isEqualTo(new TimeSlot(TODAY_12PM, TODAY_2PM));
            assertThat(slots.get(2)).isEqualTo(new TimeSlot(TODAY_3PM, TODAY_5PM));
        }

        @Test
        @DisplayName("Event starts at range start → only free slot after event")
        void eventAtRangeStart_onlySlotAfter() {
            Event event = buildEvent(1L, "Early", TODAY_9AM, TODAY_10AM, EventType.APPOINTMENT, 1L);
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(event));

            List<TimeSlot> slots = eventService.getAvailability(req);

            assertThat(slots).hasSize(1);
            assertThat(slots.getFirst()).isEqualTo(new TimeSlot(TODAY_10AM, TODAY_5PM));
        }

        @Test
        @DisplayName("Event ends at range end → only free slot before event")
        void eventAtRangeEnd_onlySlotBefore() {
            Event event = buildEvent(1L, "Late", TODAY_3PM, TODAY_5PM, EventType.APPOINTMENT, 1L);
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(event));

            List<TimeSlot> slots = eventService.getAvailability(req);

            assertThat(slots).hasSize(1);
            assertThat(slots.getFirst()).isEqualTo(new TimeSlot(TODAY_9AM, TODAY_3PM));
        }

        @Test
        @DisplayName("BLOCK events also reduce availability")
        void blockEventsReduceAvailability() {
            Event block = buildEvent(1L, "Lunch", TODAY_12PM, TODAY_1PM, EventType.BLOCK, 1L);
            AvailabilityRequest req = new AvailabilityRequest(1L, TODAY_9AM, TODAY_5PM);
            when(eventRepository.findByOwnerIdAndStartAtBeforeAndEndAtAfterOrderByStartAtAsc(1L, TODAY_5PM, TODAY_9AM))
                    .thenReturn(List.of(block));

            List<TimeSlot> slots = eventService.getAvailability(req);

            assertThat(slots).hasSize(2);
            assertThat(slots.get(0)).isEqualTo(new TimeSlot(TODAY_9AM, TODAY_12PM));
            assertThat(slots.get(1)).isEqualTo(new TimeSlot(TODAY_1PM, TODAY_5PM));
        }
    }
}

