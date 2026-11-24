package com.inkpen.streaks.config;

import com.inkpen.streaks.model.EventRecord;
import com.inkpen.streaks.model.StreakResponse;
import com.inkpen.streaks.model.UserProfile;
import com.inkpen.streaks.service.StreakEngineService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class DataLoader implements CommandLineRunner {

    private final StreakEngineService engine;

    @Value("${streaks.data.dir:data}")
    private String dataDir;

    @Value("${streaks.output.csv:streaks_as_of_2026-11-15T1000IST.csv}")
    private String outputCsv;

    public DataLoader(StreakEngineService engine) {
        this.engine = engine;
    }

    @Override
    public void run(String... args) throws Exception {
        Path base = Path.of(dataDir);
        loadUsers(base.resolve("users.csv"));
        if (Files.exists(base.resolve("videos.csv"))) {
            loadVideos(base.resolve("videos.csv"));
        }
        loadEvents(base.resolve("events.csv"));
        exportSnapshotCsv(Path.of(outputCsv));
    }

    private void loadUsers(Path path) throws Exception {
        if (!Files.exists(path)) return;
        try (Reader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord r : parser) {
                engine.registerUser(new UserProfile(
                        r.get("user_id"),
                        r.get("name"),
                        OffsetDateTime.parse(r.get("signup_at")).toInstant(),
                        ZoneId.of(r.get("home_tz")),
                        r.get("plan"),
                        Integer.parseInt(r.get("freeze_balance")),
                        Integer.parseInt(r.get("streak_as_of_2026_10_31"))
                ));
            }
        }
    }

    private void loadVideos(Path path) throws Exception {
        try (Reader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord r : parser) {
                String pub = r.isMapped("published_at") ? r.get("published_at") : r.get("publish_time");
                engine.registerVideoPublishTime(r.get("video_id"), OffsetDateTime.parse(pub).toInstant());
            }
        }
    }

    private void loadEvents(Path path) throws Exception {
        if (!Files.exists(path)) return;
        try (Reader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord r : parser) {
                String ws = r.get("watched_s");
                engine.ingestEvent(new EventRecord(
                        r.get("event_id"),
                        r.get("user_id"),
                        r.get("event_type"),
                        emptyToNull(r.get("video_id")),
                        (ws == null || ws.isBlank()) ? null : Double.parseDouble(ws),
                        r.get("client_ts"),
                        r.get("device_tz"),
                        r.get("received_at"),
                        r.get("app_version"),
                        emptyToNull(r.get("meta"))
                ));
            }
        }
    }

    public void exportSnapshotCsv(Path outPath) throws Exception {
        Instant asOf = OffsetDateTime.parse("2026-11-15T10:00:00+05:30").toInstant();
        try (FileWriter writer = new FileWriter(outPath.toFile());
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("user_id", "current_streak", "status", "reason").build())) {
            for (UserProfile u : engine.getAllUsers()) {
                StreakResponse resp = engine.getStreakAsOf(u.userId(), asOf);
                printer.printRecord(resp.userId(), resp.currentStreak(), resp.status(), resp.reason());
            }
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
