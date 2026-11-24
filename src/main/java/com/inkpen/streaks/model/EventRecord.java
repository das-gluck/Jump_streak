package com.inkpen.streaks.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.OffsetDateTime;

public record EventRecord(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("video_id") String videoId,
        @JsonProperty("watched_s") Double watchedS,
        @JsonProperty("client_ts") String clientTs,
        @JsonProperty("device_tz") String deviceTz,
        @JsonProperty("received_at") String receivedAt,
        @JsonProperty("app_version") String appVersion,
        @JsonProperty("meta") String meta
) {
    public Instant clientInstant() {
        return OffsetDateTime.parse(clientTs).toInstant();
    }

    public Instant receivedInstant() {
        return Instant.parse(receivedAt);
    }
}
