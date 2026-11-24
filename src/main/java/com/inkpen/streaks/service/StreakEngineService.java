package com.inkpen.streaks.service;

import com.inkpen.streaks.model.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreakEngineService {

    // =========================================================================
    // POLICY CONSTANTS (Change MAX_OFFLINE_DELAY_HOURS to 36.0 in recording)
    // =========================================================================
    public static double MAX_OFFLINE_DELAY_HOURS = 168.0; // 7 days (7 * 24h)
    public static final int MIN_WATCH_SECONDS = 60;          // 60s cumulative daily watch threshold
    public static final LocalDate EVALUATION_START = LocalDate.of(2026, 11, 1);
    public static final LocalDate OUTAGE_DATE = LocalDate.of(2026, 11, 9);

    // Affected users from incident_2026-11-09.md LB 503 logs
    private static final Set<String> OUTAGE_AFFECTED_UIDS = Set.of("u08", "u12", "u31877", "u55120");

    private final Map<String, UserProfile> users = new ConcurrentHashMap<>();
    private final Map<String, EventRecord> eventsById = new ConcurrentHashMap<>();
    private final Set<String> naturalEventKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> videoPublishTimes = new ConcurrentHashMap<>();

    public void registerUser(UserProfile user) {
        users.put(user.userId(), user);
    }

    public UserProfile getUser(String userId) {
        return users.get(userId);
    }

    public Collection<UserProfile> getAllUsers() {
        return users.values().stream()
                .sorted(Comparator.comparing(UserProfile::userId))
                .toList();
    }

    public void registerVideoPublishTime(String videoId, Instant publishedAt) {
        videoPublishTimes.put(videoId, publishedAt);
    }

    /**
     * Idempotent ingestion: safe against duplicate event_ids AND app 3.1.x retry-regenerated IDs.
     * Result is completely independent of arrival order.
     */
    public synchronized boolean ingestEvent(EventRecord raw) {
        String effectiveReceivedAt = (raw.receivedAt() == null || raw.receivedAt().isBlank())
                ? Instant.now().toString()
                : raw.receivedAt();

        EventRecord event = new EventRecord(
                raw.eventId(),
                raw.userId(),
                raw.eventType(),
                raw.videoId(),
                raw.watchedS(),
                raw.clientTs(),
                raw.deviceTz(),
                effectiveReceivedAt,
                raw.appVersion(),
                raw.meta()
        );

        if (event.eventId() != null && eventsById.containsKey(event.eventId())) {
            return false; // Duplicate event_id
        }

        // Composite natural key prevents double-counting when app 3.1.4 regenerates event_ids on retry
        String naturalKey = String.join("|",
                nullToEmpty(event.userId()),
                nullToEmpty(event.eventType()),
                nullToEmpty(event.videoId()),
                nullToEmpty(event.clientTs()),
                event.watchedS() == null ? "" : String.valueOf(event.watchedS().intValue())
        );
        if (!naturalEventKeys.add(naturalKey)) {
            return false;
        }

        String id = (event.eventId() == null || event.eventId().isBlank())
                ? "gen_" + UUID.randomUUID()
                : event.eventId();
        eventsById.put(id, event);
        return true;
    }

    public StreakResponse getStreakAsOf(String userId, Instant asOf) {
        UserProfile user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + userId);
        }

        ZoneId currentTz = resolveLatestTimezone(user, asOf);
        LocalDate asOfLocalDate = ZonedDateTime.ofInstant(asOf, currentTz).toLocalDate();
        LocalDate endEvaluationDate = asOfLocalDate.minusDays(1); // Completed days prior to current day

        List<DayRecord> days = evaluateDays(user, EVALUATION_START, asOfLocalDate, asOf);

        // Check if user already completed today (asOfLocalDate)
        DayRecord todayRecord = days.isEmpty() ? null : days.get(days.size() - 1);
        int currentStreak = user.streakAsOf20261031();
        for (DayRecord d : days) {
            if (!d.date().isAfter(endEvaluationDate)) {
                currentStreak = d.streakAfterDay();
            }
        }
        if (todayRecord != null && todayRecord.date().equals(asOfLocalDate)
                && "COMPLETED".equals(todayRecord.reasonCode())) {
            currentStreak = todayRecord.streakAfterDay();
        }

        int remainingFreezes = countRemainingFreezes(user, days);
        String status;
        String reason;

        if (currentStreak == 0) {
            status = "BROKEN";
            DayRecord lastMissed = findLastNonBridgedMiss(days);
            reason = lastMissed != null
                    ? "Streak broken on " + lastMissed.date() + " (" + lastMissed.reasonCode() + ")"
                    : "No active streak";
        } else if (todayRecord != null && todayRecord.date().equals(asOfLocalDate)
                && "COMPLETED".equals(todayRecord.reasonCode())) {
            status = "ACTIVE";
            reason = "Completed lesson today (" + todayRecord.watchedSeconds() + "s watched)";
        } else {
            status = "ACTIVE_PENDING_TODAY";
            reason = "Active streak of " + currentStreak + " days; awaiting today's lesson (" + asOfLocalDate + ")";
        }

        return new StreakResponse(userId, currentStreak, status, reason, remainingFreezes);
    }

    public List<DayRecord> getDays(String userId, LocalDate from, LocalDate to, Instant asOf) {
        UserProfile user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + userId);
        }
        LocalDate simStart = from.isBefore(EVALUATION_START) ? from : EVALUATION_START;
        List<DayRecord> all = evaluateDays(user, simStart, to, asOf);
        return all.stream()
                .filter(d -> !d.date().isBefore(from) && !d.date().isAfter(to))
                .toList();
    }

    /**
     * Core deterministic day-by-day evaluation engine.
     */
    private List<DayRecord> evaluateDays(UserProfile user, LocalDate from, LocalDate to, Instant asOf) {
        List<EventRecord> userEvents = eventsById.values().stream()
                .filter(e -> e.userId().equals(user.userId()))
                .filter(e -> !e.receivedInstant().isAfter(asOf))
                .sorted(Comparator.comparing(EventRecord::clientInstant))
                .toList();

        Map<LocalDate, Integer> validWatchedByDate = new HashMap<>();
        Map<LocalDate, List<String>> notesByDate = new HashMap<>();
        Map<LocalDate, String> rejectionByDate = new HashMap<>();
        Set<LocalDate> explicitFreezeDates = new HashSet<>();
        int purchasedFreezes = 0;

        for (EventRecord e : userEvents) {
            if ("freeze_purchase".equals(e.eventType())) {
                purchasedFreezes++;
            } else if ("freeze_applied".equals(e.eventType()) && e.meta() != null) {
                String forDate = parseMetaValue(e.meta(), "for_date");
                if (forDate != null) {
                    explicitFreezeDates.add(LocalDate.parse(forDate));
                }
            }
        }

        for (EventRecord e : userEvents) {
            if (!"video_end".equals(e.eventType())) continue;

            // =====================================================================
            // CALENDAR DAY ASSIGNMENT (Shown in Step 1 of Screen Recording)
            // =====================================================================
            EventRecord matchingStart = findMatchingVideoStart(userEvents, e);
            EventRecord anchorEvent = (matchingStart != null) ? matchingStart : e;
            ZoneId eventZone = parseZone(anchorEvent.deviceTz(), user.homeTz());
            LocalDate assignedDate = ZonedDateTime.ofInstant(anchorEvent.clientInstant(), eventZone).toLocalDate();

            double delayHours = Duration.between(e.clientInstant(), e.receivedInstant()).toSeconds() / 3600.0;
            int watched = e.watchedS() != null ? e.watchedS().intValue() : 0;

            // 1. Future client clock check
            if (delayHours < -0.25) {
                rejectionByDate.putIfAbsent(assignedDate, "REJECTED_CLOCK_TAMPER");
                notesByDate.computeIfAbsent(assignedDate, k -> new ArrayList<>())
                        .add("Rejected: client_ts is in the future relative to server received_at");
                continue;
            }

            // 2. Video publish timestamp check (when videos.csv is loaded)
            if (e.videoId() != null && videoPublishTimes.containsKey(e.videoId())) {
                Instant publishedAt = videoPublishTimes.get(e.videoId());
                if (anchorEvent.clientInstant().isBefore(publishedAt)) {
                    rejectionByDate.putIfAbsent(assignedDate, "REJECTED_CLOCK_TAMPER");
                    notesByDate.computeIfAbsent(assignedDate, k -> new ArrayList<>())
                            .add("Rejected: video " + e.videoId() + " published after claimed client_ts");
                    continue;
                }
            }

            // 3. Offline queue_depth integrity check for app 3.2.0+
         // 3. Offline queue_depth integrity check for app 3.2.0+
            if (delayHours > 1.0 && isVersionAtLeast320(e.appVersion())) {
                boolean hasValidQueueProof = hasQueueDepthProof(userEvents, anchorEvent.clientInstant(), e.receivedInstant());
                if (!hasValidQueueProof) {
                    rejectionByDate.putIfAbsent(assignedDate, "REJECTED_CLOCK_TAMPER");
                    notesByDate.computeIfAbsent(assignedDate, k -> new ArrayList<>())
                            .add(String.format("Rejected: %.1fh late sync on v%s but online app_open showed queue_depth=0",
                                    delayHours, e.appVersion()));
                    continue;
                }
            }

            // 4. Maximum offline sync window check (168h = 7 days default; 36h in demo)
            if (delayHours > MAX_OFFLINE_DELAY_HOURS) {
                rejectionByDate.putIfAbsent(assignedDate, "REJECTED_LATE_SYNC");
                notesByDate.computeIfAbsent(assignedDate, k -> new ArrayList<>())
                        .add(String.format("Rejected: synced %.2fh late (exceeds %.0fh offline limit)",
                                delayHours, MAX_OFFLINE_DELAY_HOURS));
                continue;
            }

            // Valid watch event
            validWatchedByDate.merge(assignedDate, watched, Integer::sum);
            if (delayHours > 1.0) {
                notesByDate.computeIfAbsent(assignedDate, k -> new ArrayList<>())
                        .add(String.format("Offline lesson (%s, %ds) synced %.2fh late",
                                e.videoId(), watched, delayHours));
            } else {
                notesByDate.computeIfAbsent(assignedDate, k -> new ArrayList<>())
                        .add(String.format("Watched %s (%ds) in %s", e.videoId(), watched, eventZone));
            }
        }

        // Check for incomplete video_start without video_end (e.g., u11 on Nov 11)
        for (EventRecord e : userEvents) {
            if ("video_start".equals(e.eventType()) && !hasMatchingVideoEnd(userEvents, e)) {
                ZoneId z = parseZone(e.deviceTz(), user.homeTz());
                LocalDate d = ZonedDateTime.ofInstant(e.clientInstant(), z).toLocalDate();
                notesByDate.computeIfAbsent(d, k -> new ArrayList<>())
                        .add("Incomplete playback: video_start (" + e.videoId() + ") had no video_end (app stopped/crashed)");
            }
        }

        // Walk day-by-day from EVALUATION_START to `to`
        int runningStreak = user.streakAsOf20261031();
        int availableFreezes = user.freezeBalance() + purchasedFreezes;
        List<DayRecord> result = new ArrayList<>();

        LocalDate todayInHome = ZonedDateTime.ofInstant(asOf, resolveLatestTimezone(user, asOf)).toLocalDate();

        for (LocalDate date = EVALUATION_START; !date.isAfter(to); date = date.plusDays(1)) {
            int watched = validWatchedByDate.getOrDefault(date, 0);
            List<String> notes = new ArrayList<>(notesByDate.getOrDefault(date, List.of()));
            LocalDate signupDate = ZonedDateTime.ofInstant(user.signupAt(), user.homeTz()).toLocalDate();

            String reasonCode;
            String agentAction = "No action needed.";

            if (date.isBefore(signupDate)) {
                reasonCode = "PRE_SIGNUP";
                notes.add("User had not signed up yet");
            } else if (watched >= MIN_WATCH_SECONDS) {
                runningStreak += 1;
                reasonCode = "COMPLETED";
                if (explicitFreezeDates.contains(date) && purchasedFreezes > 0) {
                    agentAction = "REFUND ₹49: User bought/applied freeze for this date before offline lesson synced.";
                }
            } else if (date.equals(todayInHome)) {
                reasonCode = "PENDING_TODAY";
                notes.add("Day still in progress as of " + asOf);
            } else if (date.equals(OUTAGE_DATE) && OUTAGE_AFFECTED_UIDS.contains(user.userId())) {
                reasonCode = "OUTAGE_EXCUSED";
                notes.add("9 Nov 503 ingestion outage confirmed in LB logs; streak bridged (+0 days)");
                agentAction = "Confirm streak bridged due to 9 Nov outage (no freeze deducted).";
            } else if (watched > 0) {
                if (availableFreezes > 0 && runningStreak > 0) {
                    availableFreezes--;
                    reasonCode = "FROZEN_AUTO";
                    notes.add("Only " + watched + "s watched (<" + MIN_WATCH_SECONDS + "s); freeze applied to bridge streak");
                } else {
                    runningStreak = 0;
                    reasonCode = "INSUFFICIENT_WATCH_TIME";
                    notes.add("Watched " + watched + "s total (minimum " + MIN_WATCH_SECONDS + "s required)");
                    agentAction = "Explain 60s daily learning rule (short 3-7s video swipes do not count).";
                }
            } else if (rejectionByDate.containsKey(date)) {
                runningStreak = 0;
                reasonCode = rejectionByDate.get(date);
                agentAction = "REJECTED_CLOCK_TAMPER".equals(reasonCode)
                        ? "Do NOT restore: device reported queue_depth=0 prior to backdated sync."
                        : "Do NOT restore: offline sync exceeded maximum allowed delay.";
            } else if (availableFreezes > 0 && runningStreak > 0) {
                availableFreezes--;
                reasonCode = "FROZEN_AUTO";
                if (isDatelineJumpSkip(userEvents, date, user.homeTz())) {
                    notes.add("Calendar date skipped during eastbound International Date Line flight; freeze bridged streak");
                } else {
                    notes.add("No lesson recorded; streak freeze used to bridge streak (+0 days)");
                }
            } else {
                runningStreak = 0;
                reasonCode = "MISSED";
                if (notes.isEmpty()) {
                    notes.add("No learning activity recorded");
                }
                agentAction = "Valid miss (0 freezes available). Offer empathy; do not restore unless user has unrecorded outage proof.";
            }

            result.add(new DayRecord(date, reasonCode, watched, runningStreak, notes, agentAction));
        }

        return result;
    }

    private EventRecord findMatchingVideoStart(List<EventRecord> userEvents, EventRecord endEvent) {
        if (endEvent.videoId() == null) return null;
        return userEvents.stream()
                .filter(e -> "video_start".equals(e.eventType()))
                .filter(e -> endEvent.videoId().equals(e.videoId()))
                .filter(e -> !e.clientInstant().isAfter(endEvent.clientInstant()))
                .filter(e -> Duration.between(e.clientInstant(), endEvent.clientInstant()).toMinutes() <= 30)
                .max(Comparator.comparing(EventRecord::clientInstant))
                .orElse(null);
    }
    
    private boolean hasQueueDepthProof(List<EventRecord> userEvents, Instant offlineClientTs, Instant batchReceivedAt) {
        return userEvents.stream()
                .filter(e -> "app_open".equals(e.eventType()))
                // Must be a real-time online app_open (not a queued offline event)
                .filter(e -> Math.abs(Duration.between(e.clientInstant(), e.receivedInstant()).toSeconds()) < 60)
                // Must occur after the offline lesson was watched and up to the sync flush
                .filter(e -> !e.receivedInstant().isBefore(offlineClientTs)
                        && !e.receivedInstant().isAfter(batchReceivedAt.plusSeconds(60)))
                .anyMatch(e -> {
                    String qd = parseMetaValue(e.meta(), "queue_depth");
                    return qd != null && Integer.parseInt(qd) > 0;
                });
    }

    private boolean hasMatchingVideoEnd(List<EventRecord> userEvents, EventRecord startEvent) {
        if (startEvent.videoId() == null) return false;
        return userEvents.stream()
                .filter(e -> "video_end".equals(e.eventType()))
                .filter(e -> startEvent.videoId().equals(e.videoId()))
                .filter(e -> !e.clientInstant().isBefore(startEvent.clientInstant()))
                .anyMatch(e -> Duration.between(startEvent.clientInstant(), e.clientInstant()).toMinutes() <= 30);
    }

    private boolean hasQueueDepthProof(List<EventRecord> userEvents, Instant batchReceivedAt) {
        return userEvents.stream()
                .filter(e -> "app_open".equals(e.eventType()))
                .filter(e -> Math.abs(Duration.between(e.clientInstant(), e.receivedInstant()).toSeconds()) < 60)
                .filter(e -> Math.abs(Duration.between(e.receivedInstant(), batchReceivedAt).toSeconds()) <= 300)
                .anyMatch(e -> {
                    String qd = parseMetaValue(e.meta(), "queue_depth");
                    return qd != null && Integer.parseInt(qd) > 0;
                });
    }

    private boolean isDatelineJumpSkip(List<EventRecord> userEvents, LocalDate date, ZoneId homeTz) {
        return userEvents.stream()
                .filter(e -> "tz_change".equals(e.eventType()))
                .anyMatch(e -> {
                    ZoneId z = parseZone(e.deviceTz(), homeTz);
                    LocalDate changeDate = ZonedDateTime.ofInstant(e.clientInstant(), z).toLocalDate();
                    return changeDate.equals(date.plusDays(1));
                });
    }

    private ZoneId resolveLatestTimezone(UserProfile user, Instant asOf) {
        return eventsById.values().stream()
                .filter(e -> e.userId().equals(user.userId()))
                .filter(e -> !e.receivedInstant().isAfter(asOf))
                .filter(e -> e.deviceTz() != null && !e.deviceTz().isBlank())
                .max(Comparator.comparing(EventRecord::clientInstant))
                .map(e -> parseZone(e.deviceTz(), user.homeTz()))
                .orElse(user.homeTz());
    }

    private int countRemainingFreezes(UserProfile user, List<DayRecord> days) {
        long purchased = eventsById.values().stream()
                .filter(e -> e.userId().equals(user.userId()) && "freeze_purchase".equals(e.eventType()))
                .count();
        long used = days.stream().filter(d -> "FROZEN_AUTO".equals(d.reasonCode())).count();
        return Math.max(0, user.freezeBalance() + (int) purchased - (int) used);
    }

    private DayRecord findLastNonBridgedMiss(List<DayRecord> days) {
        for (int i = days.size() - 1; i >= 0; i--) {
            if (days.get(i).streakAfterDay() == 0 && !"PRE_SIGNUP".equals(days.get(i).reasonCode())) {
                return days.get(i);
            }
        }
        return null;
    }

    private ZoneId parseZone(String tz, ZoneId fallback) {
        try {
            return (tz == null || tz.isBlank()) ? fallback : ZoneId.of(tz);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean isVersionAtLeast320(String version) {
        if (version == null) return false;
        return version.compareTo("3.2.0") >= 0;
    }

    private String parseMetaValue(String meta, String key) {
        if (meta == null) return null;
        for (String part : meta.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].trim().equals(key)) {
                return kv[1].trim();
            }
        }
        return null;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
