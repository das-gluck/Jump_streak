package com.inkpen.streaks.model;

import java.time.Instant;
import java.time.ZoneId;

public record UserProfile(
        String userId,
        String name,
        Instant signupAt,
        ZoneId homeTz,
        String plan,
        int freezeBalance,
        int streakAsOf20261031
) {}
