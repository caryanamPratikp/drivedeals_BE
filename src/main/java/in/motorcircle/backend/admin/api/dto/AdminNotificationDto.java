package in.motorcircle.backend.admin.api.dto;

import java.time.Instant;

public record AdminNotificationDto(
    String id,
    String type,
    String title,
    String message,
    String time,
    boolean read,
    Instant timestamp
) {}
