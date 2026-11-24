# Streaks-v2 Service (Spring Boot)

## Prerequisites
- Java 17+ and Maven 3.8+

## Quick Start (< 2 minutes)
1. Ensure `users.csv` and `events.csv` are inside `./data/` (already bundled in this zip).
2. Run the application (automatically ingests `./data/` and writes `streaks_as_of_2026-11-15T1000IST.csv`):
   ```bash
   mvn spring-boot:run
   ```
3. Run unit tests:
   ```bash
   mvn test
   ```

## Endpoints
- **Support Agent View (HTML):** `http://localhost:8080/support?userId=u07`
- **Get Streak:** `GET http://localhost:8080/users/u07/streak?as_of=2026-11-15T10:00:00+05:30`
- **Get Daily Breakdown:** `GET http://localhost:8080/users/u07/days?from=2026-11-01&to=2026-11-14`
- **Ingest Events (supports replay `received_at`):**
  ```bash
  curl -X POST http://localhost:8080/events \
    -H "Content-Type: application/json" \
    -d '[{"event_id":"e_test_1","user_id":"u01","event_type":"video_end","video_id":"v001","watched_s":120,"client_ts":"2026-11-15T08:00:00+05:30","device_tz":"Asia/Kolkata","received_at":"2026-11-15T02:30:05Z","app_version":"3.2.0"}]'
  ```

## Screen Recording Guide (36-Hour Rule Change)
1. Open `src/main/java/com/inkpen/streaks/service/StreakEngineService.java`.
2. Show `evaluateDays()` (around lines 163-168) where `anchorEvent.clientInstant()` + `deviceTz` assigns a lesson to a `LocalDate`.
3. Change `MAX_OFFLINE_DELAY_HOURS = 168.0;` (line 17) to `36.0;`.
4. Predict: **`u03` (Ananya)** drops from `26` -> `2` because her Nov 12 lesson synced `37.75h` late; `u07` (`14.75h`), `u13` (`24.10h`), and `u14` (`23.88h`) remain unchanged.
5. Restart `mvn spring-boot:run` and verify `streaks_as_of_2026-11-15T1000IST.csv` and `http://localhost:8080/support?userId=u03`.
