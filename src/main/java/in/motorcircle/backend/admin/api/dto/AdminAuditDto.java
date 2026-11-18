package in.motorcircle.backend.admin.api.dto;

import java.time.Instant;

public record AdminAuditDto(
    String id,
    String entityType,
    String entityId,
    String action,
    String actorName,
    String summary,
    Instant createdAt
) {}
