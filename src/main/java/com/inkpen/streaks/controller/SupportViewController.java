package com.inkpen.streaks.controller;

import com.inkpen.streaks.model.*;
import com.inkpen.streaks.service.StreakEngineService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
public class SupportViewController {

    private static final Instant DEFAULT_NOW = OffsetDateTime.parse("2026-11-15T10:00:00+05:30").toInstant();
    private final StreakEngineService engine;

    public SupportViewController(StreakEngineService engine) {
        this.engine = engine;
    }

    @GetMapping(value = "/support", produces = MediaType.TEXT_HTML_VALUE)
    public String supportPage(@RequestParam(value = "userId", defaultValue = "u07") String userId) {
        UserProfile user = engine.getUser(userId);
        if (user == null) {
            return "<h2>User not found: " + escape(userId) + "</h2>";
        }
        StreakResponse streak = engine.getStreakAsOf(userId, DEFAULT_NOW);
        List<DayRecord> days = engine.getDays(userId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), DEFAULT_NOW);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Jumpp Streaks Support</title>");
        sb.append("<style>body{font-family:sans-serif;margin:20px}table{border-collapse:collapse;width:100%}");
        sb.append("th,td{border:1px solid #ccc;padding:6px 10px;text-align:left}th{background:#f4f4f4}");
        sb.append(".bad{background:#ffe6e6}.ok{background:#e8f8e8}.warn{background:#fff8dc}</style></head><body>");

        sb.append("<h2>Streaks-v2 Support Inspector</h2>");
        sb.append("<form method='get' action='/support'>");
        sb.append("<label for='userId'>User: </label>");
        sb.append("<select id='userId' name='userId' onchange='this.form.submit()'>");
        for (UserProfile u : engine.getAllUsers()) {
            String sel = u.userId().equals(userId) ? " selected" : "";
            sb.append("<option value='").append(u.userId()).append("'").append(sel).append(">")
              .append(u.userId()).append(" - ").append(escape(u.name())).append("</option>");
        }
        sb.append("</select> ");
        sb.append("<input type='submit' value='Inspect'>");
        sb.append("</form><hr>");

        sb.append("<p><b>User:</b> ").append(escape(user.name())).append(" (").append(user.userId()).append(") | ")
          .append("<b>Plan:</b> ").append(user.plan()).append(" | ")
          .append("<b>Oct 31 Streak:</b> ").append(user.streakAsOf20261031()).append(" | ")
          .append("<b>Current Streak:</b> <span style='font-size:1.2em'>").append(streak.currentStreak()).append("</span> (")
          .append(streak.status()).append(") | <b>Freezes Left:</b> ").append(streak.remainingFreezes()).append("</p>");

        sb.append("<table><tr><th>Date</th><th>Reason Code</th><th>Watched (s)</th><th>Streak After</th><th>What Happened</th><th>Agent Action</th></tr>");
        for (DayRecord d : days) {
            String cls = d.reasonCode().startsWith("REJECTED") || d.reasonCode().equals("MISSED") || d.reasonCode().equals("INSUFFICIENT_WATCH_TIME")
                    ? "bad"
                    : (d.reasonCode().equals("COMPLETED") ? "ok" : "warn");
            sb.append("<tr class='").append(cls).append("'>")
              .append("<td>").append(d.date()).append("</td>")
              .append("<td><b>").append(d.reasonCode()).append("</b></td>")
              .append("<td>").append(d.watchedSeconds()).append("</td>")
              .append("<td>").append(d.streakAfterDay()).append("</td>")
              .append("<td>").append(escape(String.join("; ", d.notes()))).append("</td>")
              .append("<td><b>").append(escape(d.recommendedAgentAction())).append("</b></td>")
              .append("</tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}