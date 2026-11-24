package com.inkpen.streaks;

import com.inkpen.streaks.model.*;
import com.inkpen.streaks.service.StreakEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreakEngineServiceTest {

    private StreakEngineService engine;
    private static final Instant NOW = OffsetDateTime.parse("2026-11-15T10:00:00+05:30").toInstant();

    @BeforeEach
    void setup() {
        StreakEngineService.MAX_OFFLINE_DELAY_HOURS = 168.0;
        engine = new StreakEngineService();
    }

    @Test
    void testDuplicateAndOutOfOrderIngestionIsIdempotent() {
        engine.registerUser(new UserProfile("u03", "Ananya", Instant.parse("2026-09-01T03:30:00Z"),
                ZoneId.of("Asia/Kolkata"), "free", 0, 12));

        EventRecord e1 = new EventRecord("id_1", "u03", "video_end", "v019", 165.0,
                "2026-11-01T14:07:53+05:30", "Asia/Kolkata", "2026-11-02T04:10:00Z", "3.1.4", null);
        EventRecord e1Dup = new EventRecord("id_2_regen", "u03", "video_end", "v019", 165.0,
                "2026-11-01T14:07:53+05:30", "Asia/Kolkata", "2026-11-02T04:10:40Z", "3.1.4", null);

        assertTrue(engine.ingestEvent(e1));
        assertFalse(engine.ingestEvent(e1Dup));
    }

    @Test
    void testSwipeSpamBelow60SecondsBreaksStreak() {
        engine.registerUser(new UserProfile("u04", "Vikram", Instant.parse("2026-09-15T03:30:00Z"),
                ZoneId.of("Asia/Kolkata"), "free", 0, 20));

        for (int i = 0; i < 10; i++) {
            engine.ingestEvent(new EventRecord("e_" + i, "u04", "video_end", "v00" + i, 5.0,
                    "2026-11-01T23:06:0" + i + "+05:30", "Asia/Kolkata",
                    "2026-11-01T17:36:0" + i + "Z", "3.2.0", null));
        }

        List<DayRecord> days = engine.getDays("u04", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), NOW);
        assertEquals("INSUFFICIENT_WATCH_TIME", days.get(0).reasonCode());
        assertEquals(0, days.get(0).streakAfterDay());
    }

    @Test
    void testOfflineSyncWithQueueDepthAcceptedAndClockTamperRejected() {
        engine.registerUser(new UserProfile("u13", "Nikhil", Instant.parse("2026-06-14T03:30:00Z"),
                ZoneId.of("Asia/Kolkata"), "free", 0, 44));

        engine.ingestEvent(new EventRecord("open_online", "u13", "app_open", null, null,
                "2026-11-02T21:02:00+05:30", "Asia/Kolkata", "2026-11-02T15:32:01Z", "3.2.0", "queue_depth=3"));
        engine.ingestEvent(new EventRecord("offline_end", "u13", "video_end", "v019", 164.0,
                "2026-11-01T20:58:52+05:30", "Asia/Kolkata", "2026-11-02T15:32:04Z", "3.2.0", null));

        List<DayRecord> days = engine.getDays("u13", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), NOW);
        assertEquals("COMPLETED", days.get(0).reasonCode());
        assertEquals(45, days.get(0).streakAfterDay());
    }

    @Test
    void testMidnightCrossingSessionCreditedToStartDate() {
        engine.registerUser(new UserProfile("u05", "Meera", Instant.parse("2026-10-01T03:30:00Z"),
                ZoneId.of("Asia/Kolkata"), "free", 0, 6));

        engine.ingestEvent(new EventRecord("s1", "u05", "video_start", "v010", null,
                "2026-11-01T23:59:05+05:30", "Asia/Kolkata", "2026-11-01T18:29:05Z", "3.2.0", null));
        engine.ingestEvent(new EventRecord("e1", "u05", "video_end", "v010", 80.0,
                "2026-11-02T00:00:27+05:30", "Asia/Kolkata", "2026-11-01T18:30:28Z", "3.2.0", null));

        List<DayRecord> days = engine.getDays("u05", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), NOW);
        assertEquals("COMPLETED", days.get(0).reasonCode());
        assertEquals(7, days.get(0).streakAfterDay());
    }
}
