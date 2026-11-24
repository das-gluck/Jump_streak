package com.inkpen.streaks.controller;

import com.inkpen.streaks.model.*;
import com.inkpen.streaks.service.StreakEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class StreakApiController {

    private static final Instant DEFAULT_NOW = OffsetDateTime.parse("2026-11-15T10:00:00+05:30").toInstant();
    private final StreakEngineService engine;

    public StreakApiController(StreakEngineService engine) {
        this.engine = engine;
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> postEvents(@RequestBody List<EventRecord> events) {
        int accepted = 0;
        int duplicates = 0;
        for (EventRecord e : events) {
            if (engine.ingestEvent(e)) {
                accepted++;
            } else {
                duplicates++;
            }
        }
        return ResponseEntity.ok(Map.of("accepted", accepted, "duplicates_ignored", duplicates));
    }

    @GetMapping("/users/{id}/streak")
    public ResponseEntity<StreakResponse> getUserStreak(
            @PathVariable("id") String userId,
            @RequestParam(value = "as_of", required = false) String asOfStr) {
        return ResponseEntity.ok(engine.getStreakAsOf(userId, parseAsOf(asOfStr)));
    }

    @GetMapping("/users/{id}/days")
    public ResponseEntity<List<DayRecord>> getUserDays(
            @PathVariable("id") String userId,
            @RequestParam(value = "from", defaultValue = "2026-11-01") String fromStr,
            @RequestParam(value = "to", defaultValue = "2026-11-14") String toStr,
            @RequestParam(value = "as_of", required = false) String asOfStr) {
        return ResponseEntity.ok(engine.getDays(userId, LocalDate.parse(fromStr), LocalDate.parse(toStr), parseAsOf(asOfStr)));
    }
    
    private Instant parseAsOf(String asOfStr) {
        if (asOfStr == null || asOfStr.isBlank()) {
            return DEFAULT_NOW;
        }
        // Restore '+' if the URL query decoder converted an unencoded '+' into a space
        String normalized = asOfStr.trim().replace(" ", "+");
        return OffsetDateTime.parse(normalized).toInstant();
    }
}
