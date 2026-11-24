package com.inkpen.streaks.model;

public record StreakResponse(
        String userId,
        int currentStreak,
        String status,
        String reason,
        int remainingFreezes
) {}
