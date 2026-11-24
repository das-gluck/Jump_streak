package com.inkpen.streaks.model;

import java.time.LocalDate;
import java.util.List;

public record DayRecord(
        LocalDate date,
        String reasonCode,
        int watchedSeconds,
        int streakAfterDay,
        List<String> notes,
        String recommendedAgentAction
) {}
